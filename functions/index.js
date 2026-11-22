/**
 * Cloud Functions do CityFix: notificações push (Firebase Cloud Messaging, enunciado III)
 * e o fecho definitivo das ocorrências no servidor (enunciado II.5).
 *
 * As mensagens são só de dados ({type, incidentId, title, status}): o texto é construído na app,
 * na língua do telemóvel (CityFixMessagingService). Os tokens estão em users/{uid}.fcmToken.
 *
 * Publicar (na raiz do projeto): `firebase deploy --only functions` (precisa do plano Blaze).
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "europe-west1" });

const db = admin.firestore();
const DAY_MS = 24 * 60 * 60 * 1000;
const CLOSE_AFTER_MS = 5 * DAY_MS;
const ACTIVE = ["OPEN", "INFO_REQUESTED", "IN_PROGRESS"];

// Tokens dos utilizadores indicados (sem repetidos e sem quem não tem token)
async function tokensOf(uids) {
  const unique = [...new Set(uids.filter(Boolean))];
  if (unique.length === 0) return [];
  const docs = await Promise.all(unique.map((uid) => db.collection("users").doc(uid).get()));
  return docs.map((d) => d.get("fcmToken")).filter(Boolean);
}

// Tokens de todos os operadores
async function operatorTokens() {
  const snap = await db.collection("users").where("role", "==", "OPERATOR").get();
  return snap.docs.map((d) => d.get("fcmToken")).filter(Boolean);
}

// Quem acompanha a ocorrência: o autor e os seguidores (ocorrência de grupo)
function followersOf(incident) {
  return [incident.userId, ...(incident.followers || [])];
}

async function send(tokens, data) {
  if (tokens.length === 0) return;
  const payload = {};
  Object.entries(data).forEach(([k, v]) => { if (v !== undefined && v !== null) payload[k] = String(v); });
  const result = await admin.messaging().sendEachForMulticast({
    tokens,
    data: payload,
    android: { priority: "high" },
  });
  logger.info(`${data.type} ${data.incidentId}: ${result.successCount}/${tokens.length} enviadas`);
  // Motivo de cada envio recusado (ex.: token antigo ou inválido) e limpeza dos tokens que já não existem
  result.responses.forEach((r, i) => {
    if (r.success) return;
    const code = r.error && r.error.code;
    logger.warn(`${data.type} ${data.incidentId}: falhou ${tokens[i].slice(0, 12)}… → ${code}: ${r.error && r.error.message}`);
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
      db.collection("users").where("fcmToken", "==", tokens[i]).get()
        .then((snap) => snap.forEach((d) => d.ref.update({ fcmToken: admin.firestore.FieldValue.delete() })))
        .catch((e) => logger.error("Não foi possível limpar o token", e));
    }
  });
}

// Nova ocorrência: avisa os operadores
exports.onIncidentCreated = onDocumentCreated("incidents/{id}", async (event) => {
  const incident = event.data.data();
  await send(await operatorTokens(), { type: "NEW_INCIDENT", incidentId: event.params.id, title: incident.title });
});

// Mudanças numa ocorrência: estado, mensagens e proximidade da equipa
exports.onIncidentUpdated = onDocumentUpdated("incidents/{id}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  const id = event.params.id;
  const title = after.title;
  const citizens = followersOf(after);

  // Mudança de estado (enunciado III): a quem acompanha, exceto quem a fez.
  // Se foi o autor (pedido de esclarecimento, resposta, reabertura), avisa também os operadores.
  if (before.status !== after.status) {
    const history = after.history || [];
    const by = history.length ? history[history.length - 1].byUid : null;
    const tokens = await tokensOf(citizens.filter((uid) => uid !== by));
    if (by === after.userId) tokens.push(...(await operatorTokens()));
    await send(tokens, { type: "STATUS", incidentId: id, title, status: after.status });
  }

  // Mensagem nova (pedidos de informação e respostas): à outra parte
  if ((after.lastMessageAt || 0) > (before.lastMessageAt || 0)) {
    const sender = after.lastMessageBy;
    const fromCitizen = citizens.includes(sender);
    const tokens = fromCitizen
      ? await operatorTokens()
      : await tokensOf(citizens.filter((uid) => uid !== sender));
    await send(tokens, { type: "MESSAGE", incidentId: id, title });
  }

  // Operador a menos de 20 m (geofence): avisa os autores
  if ((after.operatorNearbyAt || 0) > (before.operatorNearbyAt || 0) && ACTIVE.includes(after.status)) {
    await send(await tokensOf(citizens), { type: "PROXIMITY", incidentId: id, title });
  }
});

// De hora a hora: aviso das 24 h antes do fecho e fecho definitivo ao fim de 5 dias
exports.closeResolvedIncidents = onSchedule("every 60 minutes", async () => {
  const now = Date.now();
  const snap = await db.collection("incidents").where("status", "==", "RESOLVED").get();
  for (const doc of snap.docs) {
    const incident = doc.data();
    if (!incident.resolvedAt) continue;
    const closesAt = incident.resolvedAt + CLOSE_AFTER_MS;
    if (now >= closesAt) {
      // Encerrado: arquivado e bloqueado (as regras recusam mais alterações)
      await doc.ref.update({
        status: "CLOSED",
        history: admin.firestore.FieldValue.arrayUnion({ status: "CLOSED", timestamp: now, byUid: "system", note: null }),
      });
    } else if (now >= closesAt - DAY_MS && incident.closingWarnedAt !== incident.resolvedAt) {
      await send(await tokensOf(followersOf(incident)), { type: "CLOSING", incidentId: doc.id, title: incident.title });
      await doc.ref.update({ closingWarnedAt: incident.resolvedAt });
    }
  }
});
