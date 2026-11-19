# CityFix – Parte B do operador/ticket (Marco)

> Escrito pelo Rodrigo a 19/11/2026, depois de fechar a Parte A.
> Entrega: **22/11/2026 23:59** · Defesa: 23–27/11.
> **Antes de começares: faz `git pull`.** A Parte A mexeu em `MainActivity.kt`, `IncidentsRepository.kt` e nos `strings.xml`. Se tiveres alterações locais nestes ficheiros, vais ter conflitos.

---

## 1. O que já está feito (Parte A – Rodrigo)

- **Ciclo de vida do ticket:** `OPEN → INFO_REQUESTED → IN_PROGRESS → RESOLVED → CLOSED`. As transições são validadas em `IncidentLifecycle.canTransition` (`model/StatusChange.kt`).
- **Histórico:** cada mudança de estado fica guardada em `history`, uma lista de `{status, timestamp, byUid, note}` no documento do incidente. No ecrã de detalhe é mostrado com badge + nota + data.
- **Ações do operador** (`ui/components/OperatorActions.kt`), com o layout do mockup v3:
  - Pedir dados: diálogo com pergunta obrigatória → muda para `INFO_REQUESTED` e envia a pergunta como mensagem.
  - Iniciar obra → `IN_PROGRESS`.
  - Resolver + prova: foto da galeria ou da câmara → `RESOLVED`. O `ProofUploadWorker` sobe a foto para `incidents/{id}/proof.jpg`.
- **Contactos do autor** para o operador: nome, telefone com botão Ligar e email.
- **Fecho automático** 5 dias depois de `RESOLVED` (`AutoCloseWorker`). Corre de 6 em 6 horas e quando a app abre.
- **Estatísticas** (`stats/global`): tempo médio de reparação, contagens de ativos e resolvidos, e reabertura.
- **Regras do Firestore já publicadas**, incluindo as das mensagens (ver secção 4).

---

## 2. O que falta (Parte B – tu)

### 2.1 Ler mensagens – `data/MessagesRepository.kt`
O ficheiro já existe e já tem `send()`. **Acrescenta no mesmo ficheiro:**
- `fun observeMessages(incidentId: String): Flow<List<Message>>`, um `callbackFlow` com `addSnapshotListener` em `incidents/{id}/messages`, ordenado por `timestamp`. Usa o `observeIncidents` do `IncidentsRepository` como modelo.
- `fun DocumentSnapshot.toMessage(): Message?`, o inverso de `toFirestoreMap()`. Atenção: `senderRole` está guardado como String (`UserRole.valueOf(...)`).

### 2.2 Secção de mensagens – `ui/components/MessagesSection.kt` (novo)
Segue o mockup v3, `docs/cityfix_mockup_v3.html`, secção "Mensagens" do detalhe:
- As mensagens do próprio utilizador ficam à direita e as do outro à esquerda. Mostra se quem escreveu é operador ou munícipe e a hora (podes usar `formatDateTime` de `IncidentHistory.kt`).
- Em baixo: `OutlinedTextField` + botão Enviar.
- Quando o incidente está `CLOSED`, o input fica desativado com uma nota a dizer que está arquivado. As regras recusam a escrita, por isso a UI tem de impedir antes.
- Composable **stateless**: recebe a lista, o texto, `onTextChange` e `onSend`.

### 2.3 ViewModel – `viewmodel/MessagesViewModel.kt` (novo)
- Uma instância por rota, tal como o `TicketViewModel`. Vê a `TicketViewModelFactory`: usa `CreationExtras` + `createSavedStateHandle()`.
- `messages: StateFlow<List<Message>>` vindo de `observeMessages(incidentId)`.
- O texto do input fica no `SavedStateHandle`, para não se perder ao rodar o ecrã.
- `send(incident)`:
  - Se o **munícipe** responde e o estado é `INFO_REQUESTED`, a mensagem é a resposta ao pedido de dados. Decide com o Rodrigo se isso devolve o ticket para `OPEN`/`IN_PROGRESS` (o enunciado não obriga). Por agora, só enviar a mensagem chega.
  - Para os outros casos, basta `messagesRepository.send(incident.id, texto)`.

### 2.4 Botão "Reabrir" (munícipe autor)
- Só aparece se `IncidentLifecycle.canReopen(incident)` for verdadeiro (RESOLVED há menos de 5 dias) **e** o utilizador for o autor.
- Abre um diálogo com **justificação obrigatória**. Depois:
  ```kotlin
  incidentsRepository.changeStatus(incident, IncidentStatus.OPEN, justificacao)
  messagesRepository.send(incident.id, justificacao)
  ```
  O `changeStatus` já trata das estatísticas (`addReopened`) e limpa o `resolvedAt`.
- Para `CLOSED` não há botão; o `LifecycleNotice` já mostra "Encerrado – arquivado e bloqueado".

### 2.5 Onde ligar tudo – `MainActivity.kt`
Na rota de detalhe já existe o espaço reservado:
```kotlin
TicketSection(
    ...,
    partB = {
        // PARTE B (Marco): secção de mensagens e botão "Reabrir" aqui.
    }
)
```
- Cria lá o `MessagesViewModel` com `viewModel(factory = ...)` e põe o `MessagesSection` + o botão Reabrir.
- O `partB` é um `ColumnScope`, por isso basta pôr os composables lá dentro.
- O `TicketSection` só é mostrado ao **autor** e aos **operadores**, por isso não precisas de esconder as mensagens dos outros.

### 2.6 Strings
Tudo em `values/strings.xml` **e** `values-pt/strings.xml`. Os apóstrofos têm de ser escapados (`\'`), senão o build falha.

---

## 3. Funções partilhadas – NÃO mudar a assinatura

| Função | Onde | Para quê |
|---|---|---|
| `changeStatus(incident: Incident, newStatus: IncidentStatus, note: String? = null): Boolean` | `IncidentsRepository` | Muda o estado, acrescenta ao `history` e atualiza as estatísticas. Devolve `false` se a transição for inválida. |
| `MessagesRepository.send(incidentId: String, text: String)` | `MessagesRepository` | Envia como o utilizador da sessão atual |
| `Message(id, senderUid, senderRole, text, timestamp)` | `model/Message.kt` | Modelo da mensagem |
| `IncidentLifecycle.canTransition / canReopen / closesAt` | `model/StatusChange.kt` | Regras do ciclo de vida |

Formato da mensagem no Firestore: `incidents/{id}/messages/{auto}` → `{ senderUid, senderRole, text, timestamp }`.
Se precisares de mais campos, **acrescenta-os com valor por omissão**. Não mudes os que já existem: o "Pedir dados" do operador já escreve neste formato.

---

## 4. Problemas que podes ter

1. **Escritas que falham sem aviso.** As escritas no Firestore são feitas sem `await` (por causa do modo offline). Se as regras recusarem, nada rebenta: a mensagem aparece e depois desaparece. Verifica no Logcat (filtro `PERMISSION_DENIED`). O que as regras publicadas permitem:
   - **ler** mensagens: só o autor do incidente ou um operador;
   - **criar** mensagem: `senderUid == auth.uid`, incidente **não** `CLOSED`, e autor ou operador;
   - **atualizar o incidente:** autor ou operador, e nunca quando já está `CLOSED`.
2. **`changeStatus` devolve `false`** se a transição não for válida, por exemplo reabrir um `CLOSED` ou um `RESOLVED` com mais de 5 dias. Nesse caso mostra um Snackbar/Toast e não envies a mensagem.
3. **O utilizador anónimo não pode ver mensagens.** As regras bloqueiam-no. Não chames `observeMessages` sem sessão, senão o listener dá erro de permissão.
4. **Ordenação:** usa `orderBy("timestamp")` na query. Com dados offline o snapshot pode chegar duas vezes (cache e depois servidor), e isso é normal.
5. **Recriação da Activity.** Testa com "Não manter atividades" nas opções de programador: o texto escrito e o diálogo de reabrir têm de sobreviver (`SavedStateHandle`). Foi exatamente este o bug do diálogo da câmara na Parte A.
6. **Conflitos de Git** em `MainActivity.kt`: faz `git pull` antes de começares e mexe só dentro do bloco `partB = { }`.
7. **O mapa aparece cinzento:** a chave do Maps no `AndroidManifest.xml` ainda está vazia. Não é bug teu.
8. **Domínio dos operadores:** está `isec.pt` tanto nas regras como no `AppConfig.OPERATOR_EMAIL_DOMAIN`. O `tz@cmcoimbra.pt` continua a ser operador porque o perfil já existia.
9. **As estatísticas** (`stats/global`) são reconstruídas quando um operador faz login. Se os números parecerem errados depois de testes, entra como operador.

---

## 5. Dados de teste no Firebase (projeto `cityfix-ad269`)

- **Munícipe:** José Cândido (`jcandido@gmail.com`), autor dos 6 incidentes.
- **Operador:** `a2024134442@isec.pt`.
- **As passwords não estão no repo (é público).** Pede-as ao Rodrigo.
- Estado atual dos incidentes:
  - Fuga de água: `IN_PROGRESS`;
  - Candeeiros apagados: `RESOLVED` com prova;
  - Buraco: `INFO_REQUESTED`, já com uma mensagem de teste do operador, o que dá jeito para testares a leitura;
  - os restantes: `OPEN`.
- Para testar o **Reabrir**, entra como José no "Candeeiros apagados". Está `RESOLVED`, por isso pode ser reaberto até 5 dias depois do `resolvedAt`.
- Para testar o **fecho automático**, muda o `resolvedAt` de um incidente na consola para há mais de 5 dias e reabre a app.

---

## 6. O que falta no resto do projeto (para dividirmos depois)

- [ ] Notificações N1–N4, incluindo a permissão `POST_NOTIFICATIONS`.
- [ ] Geofence de 20 m (O-F).
- [ ] Deteção de impacto (M-F).
- [ ] Agrupar incidentes (M-G).
- [ ] Vídeos: no reporte (M-E1/F5) e, eventualmente, na prova de resolução.
- [ ] Localização no registo (M-A).
- [ ] Permissões em runtime: localização em background e notificações.
- [ ] Testes em landscape e de acessibilidade (TalkBack, tamanhos de letra).
- [ ] Chave do Google Maps.
- [ ] Relatório em PDF.
- [ ] ZIP final sem pastas `build/`, `.gradle/` e `.idea/`.

Qualquer dúvida, fala comigo antes de mudares algo partilhado. 🙂
