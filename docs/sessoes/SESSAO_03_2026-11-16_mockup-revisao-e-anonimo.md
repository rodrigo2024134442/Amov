# Sessão 03: mockup, revisão do projeto e perfil anónimo

| | |
|---|---|
| **Data** | 2026-11-16 |
| **Quem** | Rodrigo, com o Claude |
| **Estado no fim da sessão** | Perfil anónimo completo e a correr no emulador (falta publicar a regra `stats`); revisão do projeto todo contra o enunciado; `PROGRESSO_CITYFIX.md` atualizado |

---

## 1. Mockup (`cityfix_mockup_v2.html` → v3)

- Revisto contra o enunciado: cobre os três perfis, o ciclo de vida, as notificações, as permissões, offline, EN/PT e portrait/landscape.
- Corrigido na **v3** (`cityfix_mockup_v3.html`, fora do repositório):
  - chat por ticket (antes todos os tickets mostravam a mesma conversa); o operador pode responder sem mudar o estado;
  - alerta da geofence só em "Em Resolução" e depois de o operador chegar a menos de 20 m;
  - uma mensagem do munícipe só passa o ticket a "Pedido de Informações" se estiver "Aberto";
  - o munícipe vê os anexos, a prova de resolução e o histórico; só o autor pode reabrir;
  - ocorrência de grupo com distância real (≤ 10 m, mesma categoria e situação): junta-se ao ticket existente em vez de criar outro;
  - pedido de localização no primeiro arranque (anónimo); MFA sempre obrigatório;
  - contador de públicas calculado, cabeçalho com o utilizador da sessão, textos em falta traduzidos, áreas de toque e letra maiores.
- Testado com 22 testes automáticos (jsdom) aos fluxos; o aspeto visual não foi verificado (o Tailwind vem de um CDN).

## 2. Revisão do projeto (commit `256dcbd`)

O código foi lido do GitHub (igual à cópia local do Rodrigo). Não foi compilado nesta revisão.

**Bem feito:** MVVM limpo (ecrãs stateless, ViewModels por rota), Firestore com cache offline, `MediaUploadWorker` com restrição de rede e compressão + miniatura, NIF único por transação (`nifs/{PERFIL}_{NIF}`), TOTP, categorias com ativar/desativar, textos em `strings.xml`.

**Problemas encontrados:**

| Problema | Estado |
|---|---|
| `IncidentMapper` não grava nem lê a `address` (a morada da Ficha 10 perde-se) | ✅ Corrigido nesta sessão |
| A consulta pública incluía as ocorrências `RESOLVED` | ✅ Corrigido (só ativas) |
| Textos em PT escritos no código (`"Morada: …"`, `"Localização Selecionada"`, `"Lat: … Lng"`) | ✅ Corrigido (`strings.xml`) |
| Chave do Google Maps vazia no `AndroidManifest.xml` (mapas cinzentos) | ⬜ Lembrete |
| `AppConfig` mudou o domínio dos operadores para `isec.pt` (os alunos também o têm); sem verificação do e-mail | ⬜ Lembrete: decidir |
| O autor pode editar e apagar a ocorrência em qualquer estado | ⬜ Por fazer (M-E1) |
| O registo do munícipe não guarda a localização (o operador vê "Localização por definir") | ⬜ Por fazer (M-A) |
| Só imagens, sem vídeos | ⬜ Por fazer (M-E1, F5) |
| Privacidade: o anónimo recebe o documento inteiro (com `userId` e coordenadas); a app só esconde no ecrã | Limitação para o relatório |
| O 2.º fator só fica registado no telemóvel (SharedPreferences); as regras do Firestore não o verificam | Limitação para o relatório |

**Por fazer** (ver as tabelas do `PROGRESSO_CITYFIX.md`): ciclo de vida do operador, mensagens, reabrir e fecho automático, notificações, deteção de impactos, ocorrência de grupo, geofence de 20 m, vídeos, permissões em segundo plano, relatório.

## 3. Perfil anónimo (enunciado I.1)

### Decisões

| Decisão | Porquê |
|---|---|
| "Nº total de ocorrências" vem de um documento **`stats/global`** com contadores | O anónimo só pode ler as públicas, mas o total inclui as privadas. Sem Cloud Functions: `FieldValue.increment()` no **mesmo WriteBatch** que grava/apaga a ocorrência (funciona offline). Quando um operador entra, `rebuild()` recalcula tudo a partir das ocorrências |
| Barra de navegação **Painel / Ocorrências** para todos os perfis (inferior em portrait, lateral em landscape) | Segue o mockup; o munícipe registado também tem o dashboard (I.2.D) |
| O anónimo começa no Painel; com sessão continua a começar na lista | |
| Centro: Praça da República (40.2086, -8.4214); raio 10 km | `AppConfig` |
| Sem permissão, fora da área ou sem GPS: ecrã de bloqueio com "Tentar novamente", "Abrir definições" e "Iniciar sessão" | O login e o registo continuam acessíveis (com sessão funciona em qualquer lado) |
| Detalhe para quem não é autor nem operador: "Autor oculto" e mapa da **zona aproximada** (coordenadas arredondadas a ~1 km, zoom 12 fixo, círculo em vez de marcador, gestos desligados) | I.1.C: "mapa com zoom afastado" |
| Testes no emulador com a localização dos Extended Controls (ou `adb emu geo fix -8.4214 40.2086`) | Escolha do Rodrigo; a app fica igual à da entrega |

### Ficheiros

- **Novos:** `model/MunicipalStats.kt`, `data/StatsRepository.kt`, `data/LocationRepository.kt`, `viewmodel/GuestAreaViewModel.kt`, `viewmodel/DashboardViewModel.kt`, `viewmodel/GuestViewModelFactories.kt`, `ui/screens/DashboardScreen.kt`, `ui/screens/GuestAreaScreen.kt`, `ui/components/AppNavigation.kt`.
- **Alterados:** `AppConfig`, `CityFixApp`, `MainActivity`, `IncidentEnums` (`IncidentStatus.isActive`), `IncidentsRepository` (consulta pública, batch com estatísticas, cache por ID), `IncidentMapper` (`address`), `EditIncidentViewModel` (`isNew`), `OperatorViewModel` e `AuthViewModelFactories` (rebuild das estatísticas), `Destinations` (`DashboardRoute`), `MainTopAppBar`, `IncidentCard`, `ShowScreen`, `MapLocationPicker`, `LocationUtils`, `strings.xml` (EN e PT).
- Entregues num ZIP (`CityFix_anonimo.zip`) com um `LEIA-ME.txt` por ficheiro e o `ALTERACOES.diff`.

### Como foi testado

- O Claude não conseguiu compilar (sem acesso ao SDK Android); fez só a verificação de sintaxe do Kotlin.
- O Rodrigo aplicou o ZIP, **compilou e correu no emulador** (API 36, localização em Coimbra): verificação da área, Painel, cartões com categoria + situação + estado, barra de navegação. Os KPIs mostram "—" porque a regra `stats` ainda não está publicada.

### Para o ciclo de vida (próximo passo)

- Ao passar a `RESOLVED`: preencher `resolvedAt` e chamar `statsRepository.addResolved(batch, resolvedAt - timestamp)` no mesmo batch.
- Ao reabrir: `statsRepository.addReopened(batch, mesmoValor)`.

## 4. Regra a acrescentar no Firestore

Dentro de `match /databases/{database}/documents { … }`:

```
match /stats/{doc} {
  allow read: if true;                    // o anónimo lê o dashboard
  allow write: if request.auth != null;   // quem cria/apaga/resolve ocorrências atualiza os contadores
}
```

Depois de publicar: entrar uma vez com uma conta de operador para calcular os indicadores.

## 5. Dúvidas do `duvidas.txt` (respostas sugeridas)

- **Autenticação com QR code, ok?** É MFA a sério (TOTP, o mesmo do Google Authenticator). Confirmar com o professor; referir no relatório a limitação da secção 2.
- **A recuperação de password só funciona com e-mails reais?** Sim: o Firebase envia mesmo o e-mail, por isso só se recupera uma conta cujo e-mail se consiga ler. Para ter várias contas de teste numa só caixa, usar aliases do Gmail (`nome+cidadao1@gmail.com`, `nome+cidadao2@gmail.com`…).

## Por fazer no fim da sessão

- Publicar a regra `stats` e entrar como operador (lembrete).
- Commit das alterações do perfil anónimo e destes documentos.
- Seguir o plano do `PROGRESSO_CITYFIX.md` (ciclo de vida do operador primeiro).
