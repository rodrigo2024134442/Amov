# Sessão 02: guia ContactsApp → CityFix e Firebase

| | |
|---|---|
| **Data** | 2026-11-07 |
| **Quem** | Marco, com o Claude |
| **Estado no fim da sessão** | Guia `docs/CityFix_Guia.html` pronto (secções 1 a 3); o código ainda não foi aplicado ao projeto |

---

## 1. Material analisado

- **Slides das aulas** (`D:\Onedrive\Documentos_Onedrive\ISEC\AMOV\Prativas`): P07 (domínio, MVVM, navegação type-safe), P08 (formulários, `UiState`, repositório JSON, factories, `Application`), P09 (Photo Picker, FileProvider, câmara, Coil 3). Os slides "FixIt Project Bridge" do fim de cada aula já apontam para o trabalho: enums de estado, `SyncStatus` (PENDING_SYNC/SYNCED), `ExposedDropdownMenuBox` nas categorias, `PickMultipleVisualMedia(maxItems = 3)`, `LazyVerticalStaggeredGrid`.
- **ContactsApp da Aula 9** (`Desktop\AMOV\Aula9`): pacotes `data`, `model`, `ui/components`, `ui/navigation`, `ui/screens`, `utils`, `viewmodel`. Notado: o `deleteContact` existe no ViewModel mas o `ShowScreen` não tem o botão ligado; e o detalhe não se atualiza depois de editar (o `getContactById` não é reativo).
- **TP no GitHub**: o Rodrigo fez 3 commits desde a sessão 01 (`4e87b43`, `684dc05`, `1a26351`): modelos `User` e `Category`, dependências de navegação e serialização, rotas, `ListScreen`/`ShowScreen`/`EditScreen` (placeholder), `IncidentCard`, `MainTopAppBar`. Corresponde à etapa #1 das aulas (P07).
- **A cópia local do Marco está 3 commits atrás**, com o `.idea/misc.xml` alterado (dá conflito no pull). Não foi feito pull: o guia explica como (passo 0.3.1).

## 2. Forma de trabalhar

- O Marco escolheu, como no PWEB, um **guia passo a passo**: o grupo faz os passos no Android Studio e o Claude revê no fim de cada um.
- A meio da sessão o Marco pediu para o guia incluir também a **integração com o Firebase**, que vai tratar a seguir.

## 3. Como o guia foi testado

- O código foi escrito numa cópia do `origin/main` fora do projeto (pasta temporária), etapa a etapa, e **compilado** com o Gradle do projeto (AGP 9.4.1, Kotlin 2.4.21). O guia mostra o código exato de cada etapa.
- **Executado no emulador** (`emulator-5554`, API 24):
  - secção 1: lista ordenada com acentos, criar, editar (o detalhe atualiza), apagar com confirmação, reabrir a app (o JSON é relido);
  - secção 2: câmara, miniatura, galeria no detalhe, apagar a ocorrência apaga as fotografias, sair do formulário sem gravar apaga a fotografia nova;
  - secção 3, com um `google-services.json` **falso**: a app arranca, grava e lê da cache do Firestore (modo offline), o `MediaUploadWorker` é agendado, lê o incidente, comprime as imagens e tenta o envio (falha só porque o projeto não existe, e volta a tentar).
- **Não testado**: o envio real para o Firestore e o Storage (precisa do projeto Firebase do grupo), o Photo Picker com imagens da galeria, e os textos em português no emulador (só confirmado que compilam).
- O emulador ficou com a build de teste do CityFix instalada; basta correr a app a partir do Android Studio para a substituir.
- O emulador usado **não tem Google Play Services**. Para a secção 3 é preciso um AVD com imagem "Google Play" ou "Google APIs".

## 4. Decisões tomadas no guia

| Decisão | Porquê |
|---|---|
| Situação, alcance, estado do ticket e `SyncStatus` como **enums** | Valores fixos do enunciado; textos traduzidos em `strings.xml` |
| Textos todos em `strings.xml` (`values/` EN, `values-pt/` PT) | Enunciado V. O código do Rodrigo tinha textos em PT escritos no código |
| Texto dos enums escolhido na UI (`ui/components/Labels.kt`) | O modelo fica sem imports de Android (regra do slide 5 da P07) |
| `mediaPaths: List<String>`, até 3 imagens | Enunciado ("imagem(ns)") e slide 26 da P09 |
| Latitude/longitude `Double?` | Ainda sem GPS (aula #4) |
| Autor `"user_demo_1"` | Ainda sem autenticação |
| Categorias fixas no repositório, com "Sinalização" desativada | Testa a regra "só categorias ativas"; a gestão pelo operador vem depois |
| Ficheiros órfãos tratados em 3 sítios (gravar, `onCleared`, apagar) | Desafio I da P09, mais o caso do formulário abandonado |
| Firestore: conversão manual (`IncidentMapper`) em vez de `toObject()` | Não obriga o modelo a ter valores por omissão em tudo |
| Firestore: escrever sem `await()` | Sem rede, a escrita fica na fila local e aparece logo na lista |
| Compressão (1600 px) e miniatura (320 px) no `MediaUploadWorker` | Enunciado IV e V; o original fica no telemóvel |
| Firebase BoM 35.1.0, google-services 4.5.0, WorkManager 2.12.0, coroutines-play-services 1.11.0 | Versões mais recentes em 2026-11-07 |

## 5. Problemas encontrados

- **Compilar fora do Android Studio** falhava com "Unable to establish loopback connection": o Java não aceita a pasta temporária com nome curto (`MARCOM~1`). Resolvido com `TEMP` e `-Djdk.net.unixdomain.tmpdir` numa pasta sem espaços (`C:\Users\Public\cfxtmp`). Não afeta o Android Studio.
- **Firebase Storage exige o plano Blaze** para buckets novos (desde o fim de 2024). Fica como lembrete no `PROGRESSO_CITYFIX.md`.

## 6. Documentação

| Ficheiro | O que mudou |
|---|---|
| `docs/CityFix_Guia.html` | Novo. Guia com as secções 0 (antes de começar), 1 (P08), 2 (P09), 3 (Firebase) e 4 (ponto de situação). Inclui o diagrama de classes na secção 0.6 |
| `docs/CityFix_Diagrama_Classes.svg` | Novo. Diagrama de classes no fim da secção 3, pedido pelo Marco como no PWEB: 5 camadas (interface, apresentação, dados, Firebase/Android, modelo), setas de chamadas (descem) e de dados (sobem), e os fluxos "ler", "gravar" e "imagens". Abre no browser; segue o tema claro/escuro do sistema |
| `docs/PROGRESSO_CITYFIX.md` | Caixa "Onde estamos", lembretes, tabela nova "Passos do guia", notas nos requisitos já cobertos pelo guia |
| `docs/sessoes/SESSAO_02_2026-11-07_guia-contactsapp-e-firebase.md` | Este ficheiro |

## 7. Durante a tarde: secções 1 e 2 feitas pelo Marco, e secção 4 do guia

### Trabalho do Marco no projeto
- Feito o pull e as secções 1 e 2 do guia (no projeto local, sem commits).
- A pedido do Marco, o Claude criou vazios os ficheiros novos do guia, para ele colar o código. O `IncidentList.kt` foi renomeado para `IncidentsList.kt` (mesmo nome da classe); o guia foi atualizado.
- Dúvidas esclarecidas: no Kotlin, "ficheiro" e "classe" são ambos `.kt` (o ícone muda com o conteúdo); no Android Studio, o `values-pt/strings.xml` aparece como `strings.xml (pt)` dentro de `values`.
- Criado o projeto no Firebase (passo 3.a.1).

### Decisões
| Decisão | Porquê |
|---|---|
| **Plano Blaze**, com cartão | Para ter o Storage das imagens |
| **MFA por link enviado pelo Firebase para o e-mail** (gratuito) | Pedido do Marco (MFA por e-mail, sem custos). Password = 1.º fator; o link, validado pelo Firebase (`reauthenticate`), = 2.º fator |
| Operadores = quem se regista com e-mail **`@cmcoimbra.pt`** | Indicado pelo Marco. Não há escolha de perfil no registo |
| **NIF: só 9 dígitos** (e único por perfil) | O professor disse que a verificação é simples |
| Notificações ficam para a semana | Ainda vão ser dadas nas aulas |
| O Claude **não altera o projeto**: só acrescenta ao guia | Pedido do Marco |

### O que mudou no guia
- **Passo 3.a reescrito** em 8 pontos, com os nomes atuais da consola (confirmados na documentação do Firebase): criar o projeto, registar a app com SHA-1, SHA-256 dos dois computadores, plano Blaze com orçamento, Authentication (Email/Password + Email link), Firestore (Europa), Storage (`us-central1` para a quota gratuita), convidar o colega.
- **Nova secção 4: Autenticação, perfis e categorias** (passos 4.a a 4.k): anónimo, munícipe pendente/ativo e operador; registo com NIF único (transação com a coleção `nifs`); login com 2.º fator por link; recuperação de password; perfil; validação de munícipes pelo operador; categorias no Firestore; consultas por perfil; regras de segurança do Firestore e do Storage por perfil.
- **Novo diagrama** da secção 4: `docs/CityFix_Diagrama_Autenticacao.svg`, também no guia.
- O antigo "Ponto de situação" passou a **secção 5**.

### Como foi testado
- Confirmado que o domínio `PROJECT_ID.firebaseapp.com` publica sozinho o `assetlinks.json` a partir dos SHA-256 registados (verificado num projeto público). Por isso basta registar o SHA-256 para o link abrir a app; não é preciso deploy no Hosting.
- A secção 4 **compila** (cópia de teste, configuração falsa). **Não foi executada**: o emulador estava desligado, e o registo, o login, o link e as regras só podem ser testados com o projeto Firebase real (passo 4.k).
- Chave de debug do computador do Marco (já não é precisa com o TOTP; fica para um eventual Google Sign-In): SHA-1 `8D:66:9A:59:2E:C6:65:74:5C:D5:AC:E0:64:66:E6:84:80:9E:3D:68`, SHA-256 `4A:F0:DD:B3:FB:7F:56:20:EB:89:F8:17:21:EC:37:97:F5:D1:CA:C7:30:52:B4:66:46:F6:39:EB:07:57:D7:26`.

### Problema em aberto (resolvido na secção 8)
- ~~Testar o operador com o link por e-mail~~: resolvido com a troca para app autenticadora.

## 8. Mudança do 2.º fator: de link por e-mail para app autenticadora (TOTP)

- **Problema**: com o link por e-mail, uma conta `@cmcoimbra.pt` só entra se alguém receber o e-mail nesse domínio; ninguém do grupo nem o professor o recebe.
- **Alternativas vistas**: domínio `@isec.pt` ou lista de operadores no Firestore (com o link); MFA por SMS ou TOTP do Identity Platform (exigem e-mail verificado); SMS com o Phone Auth normal. O Marco não quis verificar o e-mail na criação da conta nem usar SMS.
- **Decisão**: **TOTP com app autenticadora** (Google/Microsoft Authenticator), implementado na app (`utils/Totp.kt`, RFC 6238). No registo aparece um QR code; em cada login, password + código de 6 dígitos. Gratuito, sem e-mails nem SMS, e `@cmcoimbra.pt` funciona sem caixa de correio. Pode ser trocado se o professor o desaconselhar.
- **Mudanças no guia**: passo 3.a simplificado (sem SHA-256 nem Email link; pontos renumerados 3.a.1 a 3.a.7); secção 4 reescrita no 2.º fator (`Session.MfaSetup`, `mfa/{uid}` no Firestore, ecrãs `MfaSetupScreen` e `MfaScreen`, regra nova para `mfa/{uid}`); sem link, o AndroidManifest deixa de mudar; diagrama da autenticação atualizado.
- **Testado**: compila; o `Totp` passou nos valores de teste da RFC 6238 (os mesmos códigos do Google Authenticator). A app não foi executada (emulador desligado; precisa do projeto Firebase real).
- **Lição**: num `strings.xml`, um apóstrofo tem de ser escrito `'`; sem isso a compilação falha ("Invalid unicode escape sequence"). Ficou como aviso no passo 4.e.

## 9. Fotografia de perfil

- Acrescentada à secção 4 do guia (opcional, no registo e no perfil): `UserAvatar` (iniciais quando não há foto) e `PhotoSourceButtons` (galeria/câmara), compressão a 512 px e envio direto para `users/{uid}/avatar.jpg` no Storage; campo `photoUrl` no perfil; regras do Firestore e do Storage atualizadas no passo 4.j.
- Compila. Não executado (como o resto da secção 4).

## Por fazer no fim da sessão

- Terminar o passo 3.a na consola (do 3.a.2 ao 3.a.7) e seguir para o 3.b.
- Pedir ao Claude a revisão das secções 1 e 2 (feitas mas não revistas).
- Decidir como partilhar o `google-services.json`.
- Secção 4 depois da 3; correr o 4.k com o projeto real (é o primeiro teste da autenticação). Mostrar o TOTP ao professor.
- Commit da pasta `docs/` (o Claude não fez commit).
