# Progresso do projeto: CityFix (Android, Kotlin)

Estado geral do trabalho prático de Arquiteturas Móveis 2026/27 (enunciado: `AMov.ProjetoKotlin_PT.pdf`), para o grupo saber, a qualquer momento, em que ponto está o projeto e quem está a fazer o quê.

> Guia: **`docs/CityFix_Guia.html`** ("CityFix: da ContactsApp ao Firebase, passo a passo"), criado na sessão 02. Segue as aulas P08 e P09 (ContactsApp da Aula 9) e acrescenta a integração com o Firebase (secção 3) e a autenticação, perfis e categorias (secção 4). A tabela "Passos do guia" mostra o estado de cada passo; as tabelas "Requisitos do enunciado" mostram o que já está coberto.

---

## Onde estamos

| | |
|---|---|
| **Fase atual** | Secções 1 e 2 do guia feitas pelo Marco; projeto Firebase criado; a fazer o passo 3.a |
| **Próximo passo** | 3.a Configurar o projeto na consola (registo da app, Blaze, Authentication, Firestore, Storage) |
| **Última atualização** | 2026-11-07 |
| **Grupo** | Marco (2024114817) e Rodrigo (2024134442) |
| **Repositório** | `rodrigo2024134442/Amov` (GitHub), branch `main` |
| **Backend** | Firebase: projeto criado (2026-11-07), plano Blaze (decidido), resto do passo 3.a por fazer |
| **Entrega** | **2026-11-22 às 23:59** (−5 % por hora de atraso) |
| **Defesa funcional** | 23 a 27 de novembro, nas aulas práticas, com os dois presentes |

> Atualizar esta caixa sempre que um passo muda de estado.

### 🔔 Lembretes em aberto

- [x] ~~O projeto provavelmente não compila (falta `kotlinx-serialization`)~~. Resolvido no commit `684dc05` do Rodrigo; o `origin/main` compila (confirmado na sessão 02).
- [x] ~~A cópia local do Marco está 3 commits atrás do `origin/main`~~. Feito o pull (sessão 02).
- [ ] Levar às aulas as dúvidas do `duvidas.txt` e as ambiguidades do enunciado (sessão 01, secção 4).
- [x] ~~Decidir o plano Blaze~~. **Blaze**, para ter o Storage (sessão 02). O MFA fica gratuito (app autenticadora, TOTP).
- [ ] Decidir como partilhar o `google-services.json` (no Git só se o repositório for privado). Ver o passo 3.a.
- [ ] As regras do Firestore e do Storage em modo de teste **expiram**: pôr a data depois da defesa (passo 3.i), até serem trocadas pelas da secção 4 (passo 4.j).
- [ ] Mostrar ao professor o 2.º fator com **app autenticadora (TOTP)** e confirmar que é aceite como MFA (se não, trocar por outro).
- [ ] Instalar uma app autenticadora (Google ou Microsoft Authenticator) nos telemóveis dos dois, para os testes da secção 4.

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
| 3.a | Consola: projeto, app Android, Blaze, Authentication (e-mail/password), Firestore, Storage, convidar o colega | `app/google-services.json` | 🟡 | Marco | 2026-11-07 | **Só uma pessoa.** Projeto já criado (3.a.1); falta o resto |
| 3.b | Gradle: Firebase BoM, Firestore, Storage, WorkManager | `libs.versions.toml`, `build.gradle.kts` (Project e app) | ⬜ | | | |
| 3.c | Modelo: `mediaUrls`, `thumbnailUrl` | `model/Incident.kt` | ⬜ | | | |
| 3.d | Conversão para o Firestore | `data/IncidentMapper.kt` | ⬜ | | | |
| 3.e | Repositório sobre o Firestore | `data/IncidentsRepository.kt` | ⬜ | | | |
| 3.f | Compressão e envio em segundo plano | `utils/ImageUtils.kt`, `data/MediaUploadWorker.kt` | ⬜ | | | |
| 3.g | ViewModels | `IncidentsViewModel.kt`, `EditIncidentViewModel.kt` | ⬜ | | | |
| 3.h | Interface e Manifest | `IncidentCard.kt`, `ShowScreen.kt`, `MainActivity.kt`, `AndroidManifest.xml` | ⬜ | | | |
| 3.i | Regras de segurança temporárias | (consola) | ⬜ | | | Data de expiração depois da defesa |
| 3.j | Verificar a secção 3 | (nenhum) | ⬜ | | | Emulador com Google Play; testar offline |

### Secção 4: Autenticação, perfis e categorias

| Passo | O que se faz | Ficheiros | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|---|
| 4.a | Gradle: `firebase-auth` e `zxing-core` (QR code) | `libs.versions.toml`, `app/build.gradle.kts` | ⬜ | | | |
| 4.b | Modelo, validações e TOTP | `model/User.kt`, `Session.kt`, `AppConfig.kt`, `utils/Validators.kt`, `Totp.kt`, `QrCode.kt` | ⬜ | | | Operador = e-mail `@cmcoimbra.pt`; NIF = 9 dígitos |
| 4.c | Repositórios | `data/UserMapper.kt`, `CategoryMapper.kt`, `AuthRepository.kt`, `CategoriesRepository.kt`, `IncidentsRepository.kt`, `CityFixApp.kt` | ⬜ | | | |
| 4.d | ViewModels | `SessionViewModel`, `LoginViewModel`, `RegisterViewModel`, `OperatorViewModel`, `AuthViewModelFactories`, `IncidentsViewModel(+Factory)`, `EditIncidentViewModel(+Factory)` | ⬜ | | | |
| 4.e | Textos | `strings.xml` (EN e PT) | ⬜ | | | |
| 4.f | Rotas | `ui/navigation/Destinations.kt` | ⬜ | | | |
| 4.g | Componentes da fotografia e ecrãs novos | `UserAvatar`, `PhotoSourceButtons`, `LoginScreen`, `RegisterScreen`, `SessionScreens`, `ProfileScreen`, `ValidateUsersScreen`, `CategoriesScreen` | ⬜ | | | Fotografia de perfil opcional |
| 4.h | Barra de topo, lista e detalhe | `MainTopAppBar.kt`, `ListScreen.kt`, `ShowScreen.kt` | ⬜ | | | |
| 4.i | MainActivity | `MainActivity.kt` | ⬜ | | | O Manifest não muda |
| 4.j | Regras de segurança por perfil | (consola) | ⬜ | | | Substituem as do 3.i |
| 4.k | Verificar a secção 4 | (nenhum) | ⬜ | | | **App não executada** (só compilada; o `Totp` passou nos testes da RFC 6238) |

---

## Requisitos do enunciado

### I.1 Munícipe anónimo (sem login, a menos de 10 km da Praça da República)

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| A1 | Verificar a distância à Praça da República (10 km) | ⬜ | | | Exige permissão de localização |
| A2 | Dashboard: tempo médio de reparação, nº total de ocorrências, % de ativas | ⬜ | | | |
| A3 | Lista de ocorrências públicas ativas (categoria, situação, thumbnail) | ⬜ | | | Lista com miniatura: guia 1.h e 2.e; só públicas e não encerradas para o anónimo: guia 4.c |
| A4 | Detalhe público: sem dados do autor nem coordenadas; imagens e vídeos; mapa com zoom afastado | ⬜ | | | |

### I.2 Munícipe registado

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| M-A | Registo: nome, e-mail, telefone, NIF (único), localização, foto opcional, opção "Deteção de Impactos"; dados validados; fica pendente | ⬜ | | | Guia 4.b, 4.c, 4.g (com foto opcional; sem georreferenciação: GPS) |
| M-B | Login com e-mail e password, MFA e recuperação por e-mail | ⬜ | | | Ver o lembrete do plano Blaze; Guia 4.b, 4.c (2.º fator = app autenticadora, TOTP) |
| M-C | Editar perfil (sem alterar nome, NIF e localização) | ⬜ | | | Guia 4.g (`ProfileScreen`) |
| M-D | Acesso às funcionalidades do anónimo | ⬜ | | | |
| M-E1 | Abrir ocorrência: categoria ativa, situação, descrição, pelo menos 1 imagem ou vídeo, público ou privado | ⬜ | | | Guia 1.e, 1.h, 2.d, 2.g (só imagens, até 3) |
| M-E2 | Lista das minhas ocorrências, com o estado | ⬜ | | | Guia 4.d (separador "As minhas") |
| M-E3 | Detalhe, com mensagens trocadas com a câmara | ⬜ | | | |
| M-E4 | Reabrir (até 5 dias depois de resolvida) | ⬜ | | | |
| M-F | Deteção de impactos (acelerómetro, eixo vertical) e sugestão de abrir ocorrência | ⬜ | | | |
| M-G | Ocorrência de grupo: mesma categoria e situação, a menos de 10 m | ⬜ | | | |

### I.3 Operador municipal

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| O-A | Registo: e-mail do domínio da autarquia, NIF único, fica logo ativo | ⬜ | | | Guia 4.b: e-mail `@cmcoimbra.pt` = operador |
| O-B | Login com MFA e recuperação por e-mail | ⬜ | | | Igual ao M-B; Guia 4.c |
| O-C | Editar perfil (sem alterar nome e NIF) | ⬜ | | | Guia 4.g |
| O-D | Validar o registo de munícipes (nome, localização, NIF) | ⬜ | | | Guia 4.g (`ValidateUsersScreen`) |
| O-E1 | Gerir categorias: criar, editar, ativar e desativar | ⬜ | | | Guia 4.c, 4.g (`CategoriesScreen`) |
| O-E2 | Painel com todas as ocorrências (coordenadas, contactos, histórico, fotos e vídeos) | ⬜ | | | |
| O-E3 | Mudar o estado do ticket (com timestamp), pedir dados, iniciar trabalhos, carregar prova da conclusão | ⬜ | | | |
| O-F | Alerta aos autores quando o operador está a menos de 20 m de uma ocorrência ativa | ⬜ | | | Localização em segundo plano |

### II e III: Ciclo de vida do ticket e notificações

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| T1 | Estados: Aberto → Pedido de Informações → Em Resolução → Resolvido → Encerrado | ⬜ | | | |
| T2 | Fecho automático 5 dias depois de resolvido | ⬜ | | | |
| N1 | Notificação em cada mudança de estado | ⬜ | | | |
| N2 | Notificação de mensagens (pedidos de informação e respostas) | ⬜ | | | |
| N3 | Alerta de proximidade da equipa (geofence de 20 m) | ⬜ | | | |
| N4 | Aviso quando faltam 24 h para o fecho definitivo | ⬜ | | | |

### IV: Firebase e trabalho em segundo plano

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| F1 | Projeto Firebase e `google-services.json` | ⬜ | | | Guia 3.a |
| F2 | Firebase Authentication (sessão guardada no dispositivo) | ⬜ | | | Guia 3.a.5, 4.c |
| F3 | Cloud Firestore com persistência offline (ocorrências, categorias, mensagens) | ⬜ | | | Ocorrências: guia 3.d, 3.e |
| F4 | Compressão de imagens e thumbnails antes do envio | ⬜ | | | Guia 3.f (`ImageUtils`) |
| F5 | Envio de fotos e vídeos para o Firebase Storage pelo WorkManager, só com rede | ⬜ | | | Guia 3.f (`MediaUploadWorker`), só fotos |
| F6 | Firebase Cloud Messaging (push) | ⬜ | | | |

### V: Requisitos gerais

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| G1 | Jetpack Compose | 🟡 | Rodrigo | 2026-10-25 | Navegação, lista e detalhe feitos (commits do Rodrigo) |
| G2 | Arquitetura MVVM, composables stateless e stateful | 🟡 | Rodrigo | 2026-10-25 | Repositório, factories e ViewModel do formulário: guia secção 1 |
| G3 | Permissões em tempo de execução: localização (primeiro e segundo plano), câmara, notificações | ⬜ | | | |
| G4 | Inglês por omissão e português conforme o dispositivo | ⬜ | | | Guia 1.f |
| G5 | Portrait e Landscape | ⬜ | | | |
| G6 | Navegação; barras de topo e botões flutuantes conforme o contexto | ⬜ | | | |
| G7 | Acessibilidade | ⬜ | | | |

### VI: Entrega

| Req. | O que se faz | Estado | Quem | Data | Notas |
|---|---|---|---|---|---|
| E1 | Relatório técnico em PDF (soluções técnicas, opções de implementação, melhorias) | ⬜ | | | |
| E2 | ZIP `AMOV2627.2024114817.2024134442` sem `.gradle`, `build`, `app/build` e `local.properties` | ⬜ | | | Confirmar a ordem dos números |
| E3 | Entrega no Nónio até 2026-11-22 às 23:59 | ⬜ | | | |
