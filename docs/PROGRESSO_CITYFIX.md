# Progresso do projeto: CityFix (Android, Kotlin)

Estado geral do trabalho prático de Arquiteturas Móveis 2026/27 (enunciado: `AMov.ProjetoKotlin_PT.pdf`), para o grupo saber, a qualquer momento, em que ponto está o projeto e quem está a fazer o quê.

> Guia: **`docs/CityFix_Guia.html`** ("CityFix: da ContactsApp ao Firebase, passo a passo"), criado na sessão 02. Segue as aulas P08 e P09 (ContactsApp da Aula 9) e acrescenta a integração com o Firebase (secção 3) e a autenticação, perfis e categorias (secção 4). A tabela "Passos do guia" mostra o estado de cada passo; as tabelas "Requisitos do enunciado" mostram o que já está coberto.

---

## Onde estamos

| | |
|---|---|
| **Fase atual** | Secções 1 a 4 do guia, Ficha 10 (mapas), perfil anónimo e **Parte A do operador** (ciclo do ticket, prova, fecho automático) feitos |
| **Próximo passo** | **Parte B (Marco)**: mensagens e botão Reabrir, ver `docs/PARTE_B_MARCO.md`. Depois dividir o resto (notificações, geofence, impactos, grupo, vídeos, relatório) |
| **Última atualização** | 2026-11-21 |
| **Grupo** | Marco (2024114817) e Rodrigo (2024134442) |
| **Repositório** | `rodrigo2024134442/Amov` (GitHub, **público**), branch `main` |
| **Backend** | Firebase em uso: Authentication, Firestore (offline), Storage (Blaze). Falta o FCM |
| **Entrega** | **2026-11-22 às 23:59** (−5 % por hora de atraso). **Falta 1 dia** |
| **Defesa funcional** | 23 a 27 de novembro, nas aulas práticas, com os dois presentes |

> Atualizar esta caixa sempre que um passo muda de estado.

> Mockup da interface (os três perfis, portrait/landscape, EN/PT): **`docs/cityfix_mockup_v3.html`** (abrir no browser).

> Parte B do operador/ticket (o que o Marco tem de fazer, escrito pelo Rodrigo): **`docs/PARTE_B_MARCO.md`**.

### 🔔 Lembretes em aberto

- [x] ~~Publicar a regra do documento `stats/global`~~. O Rodrigo publicou as regras do Firestore, incluindo `stats` e mensagens (`PARTE_B_MARCO.md`, 19/11). As estatísticas são recalculadas quando um operador entra.
- [x] ~~Chave do Google Maps~~ (2026-11-21, Marco): chave "CityFix Maps" na Google Cloud, restrita ao Maps SDK for Android e à app (`pt.isec.amov.cityfix` + SHA-1 de debug do Marco). Fica em `secrets.properties` (raiz, fora do Git) e entra no Manifest por `manifestPlaceholders`. **O Rodrigo** tem de criar o mesmo ficheiro e acrescentar o SHA-1 dele na chave. O `secrets.properties` tem de ir no ZIP de entrega.
- [ ] **Domínio dos operadores**: o Rodrigo passou para **`@isec.pt`**, no `AppConfig.OPERATOR_EMAIL_DOMAIN` e nas regras (operador de teste dele: `a2024134442@isec.pt`). O `tz@cmcoimbra.pt` continua operador porque o perfil já existia. Atenção: com `@isec.pt` qualquer aluno fica operador. Confirmar com o Rodrigo e justificar no relatório.
- [ ] Levar às aulas as dúvidas do `duvidas.txt` (respostas sugeridas na sessão 03, secção 5).
- [ ] **2.º fator passou de TOTP para código por e-mail** (2026-11-21, Marco): `EmailCodeSender` grava em `mail/{id}` e a extensão **Trigger Email from Firestore** envia por SMTP (Gmail com palavra-passe de app). Código de 6 dígitos válido 10 min, máx. 5 tentativas. Falta: instalar a extensão, publicar a regra de `mail` e testar. Contas com e-mail fictício (ex.: `tz@cmcoimbra.pt`) deixam de conseguir entrar. Para o relatório: o código é gerado e verificado no telemóvel.
- [x] ~~As regras em modo de teste expiram~~. Já estão publicadas as regras por perfil (Firestore; confirmar também as do Storage).
- [ ] **Escritas sem aviso**: as escritas no Firestore são feitas sem `await` (modo offline); se as regras recusarem, a alteração aparece e desaparece. Ver no Logcat `PERMISSION_DENIED`.
- [ ] **Contas de teste**: o Rodrigo usa `a2024134442@isec.pt` (operador) e o José Cândido (munícipe, autor dos 6 incidentes). Passwords: pedir ao Rodrigo (não estão no repositório). Juntar ao `docs/CONTAS_DE_TESTE.md`.
- [ ] Decidir como partilhar o `google-services.json` (não está no Git, e o repositório é público: ótimo, mas tem de ir no ZIP de entrega).
- [x] ~~Decidir o plano Blaze~~. **Blaze** (sessão 02).
- [x] ~~O projeto não compila (falta `kotlinx-serialization`)~~. Resolvido no commit `684dc05`.

> Quando um lembrete estiver resolvido, marcar `[x]` e escrever a decisão no registo da sessão em que foi tomada.

---

## Como usar este ficheiro

1. Antes de começar um passo, puxar as alterações do colega (`git pull`) e ver aqui se alguém já o tem **Em curso**.
2. Pôr o passo **Em curso**, com o nome de quem o faz, e fazer commit só deste ficheiro (assim o colega vê logo).
3. Fazer o passo seguindo o guia, até ao bloco **Verificar** da secção.
4. Pôr o passo **Feito**, com a data, e fazer commit com uma mensagem curta que diga o passo.
5. Quando o Claude rever o passo, marcar **Revisto** e escrever na coluna Notas o que foi corrigido.

### Estados

| Símbolo | Estado | Significado |
|---|---|---|
| ⬜ | Por fazer | Ninguém começou |
| 🟡 | Em curso | Alguém está a fazer (ver coluna Quem) |
| ✅ | Feito | Feito, compila e corre |
| 🔍 | Revisto | Feito e revisto pelo Claude |
| ⚠️ | Com problemas | Feito mas não compila ou não funciona; ver Notas |

---

## Regras para trabalhar a dois

- **Um passo de cada vez por pessoa.** Dois passos que mexem no mesmo ficheiro (por exemplo o `MainActivity.kt`, o `AndroidManifest.xml`, o `build.gradle.kts` ou o `libs.versions.toml`) nunca ficam **Em curso** ao mesmo tempo.
- **Versões iguais.** A mesma versão do Android Studio e do Gradle. As dependências só se acrescentam no `libs.versions.toml`, e ninguém atualiza versões sozinho.
- **Um só projeto Firebase**, partilhado pelos dois (o colega é convidado no passo 3.a.7).
- **Não vão para o Git** (e não vão no ZIP de entrega): `.gradle/`, `build/`, `app/build/` e `local.properties`.
- **Textos sempre em `strings.xml`**, nunca escritos no código: inglês em `values/` e português em `values-pt/`.

---

## Passos do guia

### Secção 0: Antes de começar

| Passo | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| 0.1 a 0.6 | Ler: o que se vai construir, decisões, mapa ContactsApp → CityFix, diagrama de classes | ✅ | Marco | 2026-11-07 | Diagrama também em `docs/CityFix_Diagrama_Classes.svg` |
| 0.3.1 | Atualizar a cópia local (`git checkout -- .idea/misc.xml` + `git pull`) | ✅ | Marco | 2026-11-07 | |

### Secção 1: Formulários e persistência JSON (aula P08)

| Passo | O que se faz | Ficheiros | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|---|
| 1.a | Modelo: enums, `Incident`, `IncidentsList` com `upsert`/`remove` | `model/IncidentEnums.kt`, `IncidentsList.kt` (antigo `IncidentList.kt`), `Incident.kt` | ✅ | Marco | 2026-11-07 | Não compila até ao 1.i |
| 1.b | Repositório JSON | `data/IncidentsRepository.kt` | ✅ | Marco | 2026-11-07 | |
| 1.c | Application e Manifest | `CityFixApp.kt`, `AndroidManifest.xml` | ✅ | Marco | 2026-11-07 | |
| 1.d | ViewModel global e fábrica | `viewmodel/IncidentsViewModel.kt`, `IncidentsViewModelFactory.kt` | ✅ | Marco | 2026-11-07 | |
| 1.e | ViewModel do formulário | `viewmodel/EditIncidentUiState.kt`, `EditIncidentViewModel.kt`, `EditIncidentViewModelFactory.kt` | ✅ | Marco | 2026-11-07 | |
| 1.f | Textos EN e PT | `res/values/strings.xml`, `res/values-pt/strings.xml` | ✅ | Marco | 2026-11-07 | |
| 1.g | Componentes | `ui/components/Labels.kt`, `DropdownField.kt`, `IncidentCard.kt`, `MainTopAppBar.kt` | ✅ | Marco | 2026-11-07 | |
| 1.h | Ecrãs | `ui/screens/ListScreen.kt`, `ShowScreen.kt`, `EditScreen.kt` | ✅ | Marco | 2026-11-07 | |
| 1.i | MainActivity | `MainActivity.kt` | ✅ | Marco | 2026-11-07 | |
| 1.j | Verificar a secção 1 | (nenhum) | ✅ | Marco | 2026-11-07 | 10 verificações no guia |

### Secção 2: Fotografias, câmara e Coil 3 (aula P09)

| Passo | O que se faz | Ficheiros | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|---|
| 2.a | Dependências do Coil 3 | `libs.versions.toml`, `app/build.gradle.kts` | ✅ | Marco | 2026-11-07 | |
| 2.b | FileProvider | `res/xml/file_paths.xml`, `AndroidManifest.xml` | ✅ | Marco | 2026-11-07 | |
| 2.c | FileUtils | `utils/FileUtils.kt` | ✅ | Marco | 2026-11-07 | |
| 2.d | ViewModels: fotografias e ficheiros órfãos | `EditIncidentUiState.kt`, `EditIncidentViewModel.kt`, `IncidentsViewModel.kt` | ✅ | Marco | 2026-11-07 | |
| 2.e | Componentes de imagem | `IncidentPhotoGallery.kt`, `IncidentThumbnail.kt`, `IncidentCard.kt` | ✅ | Marco | 2026-11-07 | |
| 2.f | Textos das fotografias | `strings.xml` (EN e PT) | ✅ | Marco | 2026-11-07 | |
| 2.g | Ecrãs e MainActivity | `EditScreen.kt`, `ShowScreen.kt`, `MainActivity.kt` | ✅ | Marco | 2026-11-07 | |
| 2.h | Verificar a secção 2 | (nenhum) | ✅ | Marco | 2026-11-07 | Câmara do emulador: VirtualScene |

### Secção 3: Integração com o Firebase

| Passo | O que se faz | Ficheiros | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|---|
| 3.a a 3.j | Consola, Gradle, `IncidentMapper`, repositório no Firestore, `ImageUtils` e `MediaUploadWorker`, ViewModels, interface, regras temporárias, verificação | (ver guia) | ✅ | Marco | 2026-11-07 | Commit `b8c3f41`. Revisto na sessão 03: o `IncidentMapper` não gravava a `address` (corrigido) |

### Secção 4: Autenticação, perfis e categorias

| Passo | O que se faz | Ficheiros | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|---|
| 4.a a 4.k | Auth, TOTP, registo com NIF único, sessão, perfil, validação de munícipes, categorias, regras por perfil | (ver guia) | ✅ | Marco | 2026-11-07 | Commit `b8c3f41`. A app já corre com o projeto Firebase real (testado na sessão 03) |

### Depois do guia

| O quê | Ficheiros | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| Ficha 10: escolher a localização no mapa, morada por geocodificação inversa, mapa no detalhe | `MapLocationPicker.kt`, `LocationUtils.kt`, `EditScreen.kt`, `ShowScreen.kt` | ✅ | Rodrigo | 2026-11-14 | Commit `256dcbd`. Falta a chave do Maps (lembretes) |
| Perfil anónimo completo: 10 km, dashboard, lista e detalhe | ver sessão 03 | ✅ | Rodrigo (com o Claude) | 2026-11-16 | Commit `4cd6ef0` |
| Operador, Parte A: ciclo do ticket com histórico, pedir dados, iniciar obra, resolver com prova, fecho automático, contactos do autor | `model/StatusChange.kt`, `Message.kt`, `data/MessagesRepository.kt` (só `send`), `ProofUploadWorker.kt`, `AutoCloseWorker.kt`, `ui/components/OperatorActions.kt`, `IncidentHistory.kt`, `TicketSection.kt`, `viewmodel/TicketViewModel.kt` | ✅ | Rodrigo | 2026-11-19 | Commit `176a2be`. O munícipe só edita/apaga em `OPEN` |
| Parte B: ler mensagens, `MessagesSection`, `MessagesViewModel`, botão Reabrir, ligar no `partB = { }` do `MainActivity` | `MessagesRepository.kt` (`observeMessages`, `toMessage`), `viewmodel/MessagesViewModel.kt`, `ui/components/MessagesSection.kt`, `MainActivity.kt`, `strings.xml` | 🟡 | Marco (com o Claude) | 2026-11-21 | Testado no emulador API 35: mensagens nos dois sentidos, pedir dados, iniciar, resolver. **Falta testar o Reabrir.** Assinaturas partilhadas sem alterações |

---

## Requisitos do enunciado

Estado confirmado pela leitura do código em 2026-11-16 (sessão 03); atualizado em 2026-11-21 com a Parte A do operador (commit `176a2be`).

### I.1 Munícipe anónimo (sem login, a menos de 10 km da Praça da República)

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| A1 | Verificar a distância à Praça da República (10 km) | ✅ | Rodrigo | 2026-11-16 | `LocationRepository`, `GuestAreaViewModel`, `GuestAreaScreen`. Pede a permissão; bloqueia fora da área, sem permissão ou sem GPS |
| A2 | Dashboard: tempo médio de reparação, nº total de ocorrências, % de ativas | ✅ | Rodrigo | 2026-11-19 | `stats/global`, `DashboardScreen`. Regra publicada; o tempo médio é atualizado ao resolver (Parte A) |
| A3 | Lista de ocorrências públicas ativas (categoria, situação, thumbnail) | ✅ | Rodrigo | 2026-11-16 | Só Aberto, Pedido de Informações e Em Resolução; situação sempre visível no cartão |
| A4 | Detalhe público: sem dados do autor nem coordenadas; imagens e vídeos; mapa com zoom afastado | ✅ | Rodrigo | 2026-11-16 | "Autor oculto" e mapa da zona aproximada (~1 km, zoom 12, sem marcador). Vídeos: ver M-E1 |

### I.2 Munícipe registado

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| M-A | Registo: nome, e-mail, telefone, NIF (único), localização, foto opcional, opção "Deteção de Impactos"; dados validados; fica pendente | 🟡 | Marco | 2026-11-07 | **Falta a georreferenciação** (o operador vê "Localização por definir") |
| M-B | Login com e-mail e password, MFA e recuperação por e-mail | ✅ | Marco | 2026-11-07 | 2.º fator = app autenticadora (TOTP). Limitação para o relatório: a passagem do 2.º fator só fica guardada no telemóvel |
| M-C | Editar perfil (sem alterar nome, NIF e localização) | ✅ | Marco | 2026-11-07 | Telefone, deteção de impactos e fotografia |
| M-D | Acesso às funcionalidades do anónimo | ✅ | Rodrigo | 2026-11-16 | Separador "Painel" e separador "Públicas" na lista |
| M-E1 | Abrir ocorrência: categoria ativa, situação, descrição, pelo menos 1 imagem ou vídeo, público ou privado | 🟡 | Marco / Rodrigo | 2026-11-14 | Funciona com imagens (até 3) e localização no mapa. **Faltam os vídeos**. O autor só edita e apaga em "Aberto" (Parte A) |
| M-E2 | Lista das minhas ocorrências, com o estado | ✅ | Marco | 2026-11-07 | Separador "As minhas" |
| M-E3 | Detalhe, com mensagens trocadas com a câmara | ✅ | Rodrigo / Marco | 2026-11-21 | Mensagens em tempo real (as próprias à direita), envio desativado em `CLOSED`. Testado com munícipe e operador |
| M-E4 | Reabrir (até 5 dias depois de resolvida) | 🟡 | Marco | 2026-11-21 | Botão só para o autor com `canReopen`; justificação obrigatória (histórico + mensagem). Compila; **falta testar** |
| M-F | Deteção de impactos (acelerómetro, eixo vertical) e sugestão de abrir ocorrência | ⬜ | | | |
| M-G | Ocorrência de grupo: mesma categoria e situação, a menos de 10 m | ⬜ | | | |

### I.3 Operador municipal

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| O-A | Registo: e-mail do domínio da autarquia, NIF único, fica logo ativo | 🟡 | Marco | 2026-11-07 | Funciona, mas ver o lembrete do domínio (`isec.pt` vs `cmcoimbra.pt`) |
| O-B | Login com MFA e recuperação por e-mail | ✅ | Marco | 2026-11-07 | |
| O-C | Editar perfil (sem alterar nome e NIF) | ✅ | Marco | 2026-11-07 | |
| O-D | Validar o registo de munícipes (nome, localização, NIF) | 🟡 | Marco | 2026-11-07 | Ecrã feito; a localização fica vazia até o M-A a pedir |
| O-E1 | Gerir categorias: criar, editar, ativar e desativar | ✅ | Marco | 2026-11-07 | |
| O-E2 | Painel com todas as ocorrências (coordenadas, contactos, histórico, fotos e vídeos) | 🟡 | Rodrigo | 2026-11-19 | Coordenadas, contactos do autor (com botão Ligar) e histórico feitos. Faltam os vídeos |
| O-E3 | Mudar o estado do ticket (com timestamp), pedir dados, iniciar trabalhos, carregar prova da conclusão | ✅ | Rodrigo | 2026-11-19 | `OperatorActions`, `TicketViewModel`; prova enviada pelo `ProofUploadWorker` (`incidents/{id}/proof.jpg`). Prova só com foto |
| O-F | Alerta aos autores quando o operador está a menos de 20 m de uma ocorrência ativa | ⬜ | | | Localização em segundo plano |

### II e III: Ciclo de vida do ticket e notificações

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| T1 | Estados: Aberto → Pedido de Informações → Em Resolução → Resolvido → Encerrado | ✅ | Rodrigo | 2026-11-19 | Transições validadas em `IncidentLifecycle.canTransition`; histórico com data, autor e nota |
| T2 | Fecho automático 5 dias depois de resolvido | ✅ | Rodrigo | 2026-11-19 | `AutoCloseWorker`, de 6 em 6 horas e quando a app abre |
| N1 | Notificação em cada mudança de estado | ⬜ | | | Alternativa sem servidor: notificações locais a partir dos snapshot listeners (justificar no relatório) |
| N2 | Notificação de mensagens (pedidos de informação e respostas) | ⬜ | | | |
| N3 | Alerta de proximidade da equipa (geofence de 20 m) | ⬜ | | | |
| N4 | Aviso quando faltam 24 h para o fecho definitivo | ⬜ | | | |

### IV: Firebase e trabalho em segundo plano

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| F1 | Projeto Firebase e `google-services.json` | ✅ | Marco | 2026-11-07 | |
| F2 | Firebase Authentication (sessão guardada no dispositivo) | ✅ | Marco | 2026-11-07 | |
| F3 | Cloud Firestore com persistência offline (ocorrências, categorias, mensagens) | 🟡 | Marco / Rodrigo | 2026-11-19 | Ocorrências, categorias, utilizadores, `stats` e envio de mensagens. Falta ler as mensagens (Parte B) |
| F4 | Compressão de imagens e thumbnails antes do envio | ✅ | Marco | 2026-11-07 | `ImageUtils` (1600 px e 320 px) |
| F5 | Envio de fotos e vídeos para o Firebase Storage pelo WorkManager, só com rede | 🟡 | Marco | 2026-11-07 | Só fotos |
| F6 | Firebase Cloud Messaging (push) | ⬜ | | | |

### V: Requisitos gerais

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| G1 | Jetpack Compose | ✅ | Rodrigo / Marco | | |
| G2 | Arquitetura MVVM, composables stateless e stateful | ✅ | Rodrigo / Marco | | Ecrãs stateless, estado nos ViewModels, factories |
| G3 | Permissões em tempo de execução: localização (primeiro e segundo plano), câmara, notificações | 🟡 | Rodrigo | 2026-11-16 | Localização em primeiro plano (anónimo). Faltam segundo plano e notificações; a câmara usa o intent do sistema |
| G4 | Inglês por omissão e português conforme o dispositivo | ✅ | | | Os nomes das categorias são dados (ficam como o operador os escreve) |
| G5 | Portrait e Landscape | 🟡 | Rodrigo | 2026-11-16 | Barra lateral em landscape; falta testar todos os ecrãs |
| G6 | Navegação; barras de topo e botões flutuantes conforme o contexto | ✅ | Rodrigo | 2026-11-16 | Barra Painel / Ocorrências; barra de topo e FAB mudam com o ecrã e o perfil |
| G7 | Acessibilidade | 🟡 | | | `contentDescription` nos ícones, cartões dos KPIs lidos de uma vez. Falta rever com o TalkBack |

### VI: Entrega

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| E1 | Relatório técnico em PDF (soluções técnicas, opções de implementação, melhorias) | ⬜ | | | |
| E2 | ZIP `AMOV2627.2024114817.2024134442` sem `.gradle`, `build`, `app/build` e `local.properties` | ⬜ | | | Confirmar a ordem dos números |
| E3 | Entrega no Nónio até 2026-11-22 às 23:59 | ⬜ | | | |

---

## Plano até à entrega (sugerido na sessão 03)

> Atualização 2026-11-21: O-E3, T1 e T2 feitos pelo Rodrigo (Parte A). Mensagens e Reabrir (M-E3, M-E4, N2) passaram para o Marco (Parte B). O resto está em `PARTE_B_MARCO.md`, secção 6, para dividir.

| Dias | Pessoa A | Pessoa B |
|---|---|---|
| 17 a 18 nov | O-E3 e T1: estados com histórico e timestamp, pedir dados, iniciar, resolver com prova (foto/vídeo) | M-A: localização no registo; M-E1: vídeos e só editar em "Aberto" |
| 19 nov | M-E3 e N2: mensagens; M-E4 e T2: reabrir e fecho automático | M-G: ocorrência de grupo (10 m); M-F: deteção de impactos |
| 20 nov | N1, N3, N4: notificações (locais a partir dos listeners; FCM se houver tempo) | O-F: geofence de 20 m; G3: permissões em segundo plano e notificações |
| 21 nov | Testes completos (portrait/landscape, PT/EN, offline) | Relatório (E1) |
| 22 nov | ZIP sem `.gradle`, `build`, `app/build`, `local.properties` (E2) e entrega no Nónio (E3) | |

