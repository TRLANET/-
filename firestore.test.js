const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function dbFor(uid) {
  return testEnv.authenticatedContext(uid).firestore();
}

function unauthDb() {
  return testEnv.unauthenticatedContext().firestore();
}

test("Unauthenticated user cannot list posts or users_public", async () => {
  const db = unauthDb();
  await assertFails(db.collection("posts").where("visibility", "==", "public").get());
  await assertFails(db.collection("users_public").where("isDiscoverable", "==", true).get());
  await assertFails(db.collection("users_private").doc(ALICE_UID).get());
});

test("Public user profile creation & PII isolation", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users_private").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@secret.com",
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  await assertFails(bobDb.collection("users_private").doc(ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("users_private").doc(ALICE_UID).get());
});

test("Shadow update with ghost field on users_public is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users_public").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      username: "alice",
      bio: "",
      photoUrl: "",
      isDiscoverable: true,
      followersCount: 0,
      followingCount: 0,
      postsCount: 0,
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const aliceDb = dbFor(ALICE_UID);
  await assertFails(
    aliceDb.collection("users_public").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users_public").doc(ALICE_UID).update({
      displayName: "Alice Updated",
      updatedAt: new Date(),
    })
  );
});

test("Posts query alignment & author isolation for delete", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorHandle: "alice",
      authorPhotoUrl: "",
      content: "Hello ТЕНЬ",
      tag: "عام",
      cardStyle: "standard",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const bobDb = dbFor(BOB_UID);
  const aliceDb = dbFor(ALICE_UID);

  await assertFails(bobDb.collection("posts").get());
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("posts").doc("post_1").delete());
  await assertSucceeds(aliceDb.collection("posts").doc("post_1").delete());
});

test("Atomic like & unlike enforces likesCount consistency", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("posts").doc("post_like_test").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorHandle: "alice",
      authorPhotoUrl: "",
      content: "Like me",
      tag: "أفكار",
      cardStyle: "obsidian",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const bobDb = dbFor(BOB_UID);
  const postRef = bobDb.collection("posts").doc("post_like_test");
  const likeRef = postRef.collection("likes").doc(BOB_UID);

  await assertFails(postRef.update({ likesCount: 1, updatedAt: new Date() }));

  const batch = bobDb.batch();
  batch.set(likeRef, { userId: BOB_UID, postId: "post_like_test", createdAt: new Date() });
  batch.update(postRef, { likesCount: 1, updatedAt: new Date() });
  await assertSucceeds(batch.commit());
});

test("Direct messages are isolated to conversation participants only", async () => {
  const convId = `${ALICE_UID}_${BOB_UID}`;
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantHandles: { [ALICE_UID]: "alice", [BOB_UID]: "bob" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      lastMessage: "Hi Bob",
      lastSenderId: ALICE_UID,
      lastMessageAt: new Date(Date.now() - 5000),
      createdAt: new Date(Date.now() - 5000),
    });
    await adminDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      conversationId: convId,
      senderId: ALICE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      text: "Secret message",
      createdAt: new Date(Date.now() - 4000),
    });
  });

  const charlieDb = dbFor(CHARLIE_UID);
  const bobDb = dbFor(BOB_UID);

  await assertFails(charlieDb.collection("conversations").doc(convId).get());
  await assertFails(charlieDb.collection("conversations").doc(convId).collection("messages").doc("msg_1").get());
  await assertFails(charlieDb.collection("conversations").doc(convId).collection("messages").get());
  await assertFails(
    charlieDb.collection("conversations").doc(convId).collection("messages")
      .where("participantIds", "array-contains", BOB_UID).get()
  );

  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc(convId).collection("messages")
      .where("participantIds", "array-contains", BOB_UID).get()
  );
});
