# Sessão 01: enunciado e projeto inicial

| | |
|---|---|
| **Data** | 2026-10-26 (o projeto foi criado a 2026-10-25) |
| **Quem** | Marco, com o Claude; o Rodrigo fez o 2.º commit |
| **Estado no fim da sessão** | Enunciado analisado; projeto Android com um modelo de exemplo; à espera das aulas sobre a forma de fazer o projeto |

---

## 1. Análise do enunciado

- Enunciado: `AMov.ProjetoKotlin_PT.pdf` (**CityFix**). Entrega a **2026-11-22 às 23:59**, defesa funcional de 23 a 27 de novembro.
- App Android em **Kotlin + Jetpack Compose** para os munícipes reportarem problemas no espaço público e acompanharem a sua resolução pela câmara. O backend é o **Firebase** (Auth, Firestore, Storage, FCM), com funcionamento **offline**.
- Três perfis:
  - **Munícipe anónimo**: sem login, só a menos de 10 km da Praça da República. Dashboard, lista e detalhe de ocorrências públicas ativas, sem dados do autor;
  - **Munícipe registado**: registo com NIF único (pendente até ser validado por um operador), MFA, criar e acompanhar ocorrências, mensagens, reabrir até 5 dias, deteção de impactos com o acelerómetro;
  - **Operador municipal**: e-mail do domínio da autarquia, valida munícipes, gere categorias, vê tudo e faz avançar os tickets.
- Ciclo de vida do ticket: **Aberto → Pedido de Informações → Em Resolução → Resolvido → Encerrado** (fecha 5 dias depois de resolvido).
- Regras automáticas:
  - **ocorrência de grupo**: mesma categoria e situação a menos de 10 m;
  - **geofence de 20 m**: os autores são avisados quando um operador se aproxima de uma ocorrência ativa;
  - **aviso das 24 h** antes do fecho definitivo.
- Requisitos técnicos: MVVM (composables stateless e stateful), permissões em tempo de execução, compressão de imagens e thumbnails, envio de ficheiros pelo WorkManager, inglês por omissão e português, Portrait e Landscape, acessibilidade.
- Entrega: ZIP `AMOV2627.<nr1>.<nr2>` no Nónio, sem `.gradle`, `build`, `app/build` e `local.properties`, e um relatório técnico em PDF.
- Notado: há uma gralha na numeração dos perfis (o Operador aparece como "2." em vez de "3.").

## 2. Projeto no Android Studio

- Projeto **`CityFix`**, pacote `pt.isec.amov.cityfix`, em `Desktop\AMOV\TP`. Compose, `minSdk 24`, `targetSdk 37`, AGP 9.4.1, Kotlin 2.2.10.
- Repositório Git `rodrigo2024134442/Amov`, branch `main`:
  - `a104a95` "1st commit" (Marco, 2026-10-25): projeto criado pelo Android Studio;
  - `8f21433` (Rodrigo, 2026-10-25): modelo de exemplo.
- O que foi acrescentado no 2.º commit:

| Ficheiro | O que tem |
|---|---|
| `model/Incident.kt` | `data class` `@Serializable` com id (UUID), título, descrição, categoria, situação, âmbito, latitude, longitude, `mediaPath`, estado e timestamp |
| `model/IncidentList.kt` | Classe `IncidentsList`: lista privada, ordenação por título sem acentos nem maiúsculas (`Collator`), pesquisa por id, `add` |
| `viewmodel/IncidentsViewModel.kt` | `StateFlow` com a lista ordenada e 2 ocorrências de exemplo em Coimbra |

- O `MainActivity.kt` ainda mostra o "Hello Android" do template. Não há navegação, Firebase, permissões nem `strings.xml` em português.
- O próprio Marco reconhece que este exemplo ainda está longe do que é pedido: a forma de fazer vai ser explicada nas aulas.

## 3. Problemas encontrados (sem correção, à espera das aulas)

- **Provavelmente não compila:** o plugin `kotlin.serialization` está ativo e o `Incident` usa `@Serializable`, mas falta a biblioteca `kotlinx-serialization` nas dependências.
- **Estado, situação e âmbito são `String` livres.** O mais seguro é passarem a `enum`, para não haver valores inválidos e para os textos poderem ser traduzidos.
- **Falta informação no `Incident`:** só guarda um `mediaPath`, mas o enunciado pede várias imagens e vídeos, thumbnails, vários autores (ocorrências de grupo), mensagens e o histórico de estados com timestamp.
- O ficheiro `.idea/misc.xml` está alterado e ainda não foi feito commit.

## 4. Dúvidas (`duvidas.txt`) e respostas propostas

> São propostas: confirmar com o professor nas aulas.

| Dúvida | Resposta proposta |
|---|---|
| Como testar as distâncias (anónimo e operador)? | Simular a localização no emulador (Extended Controls > Location) ou com uma app de mock location no telemóvel. Pode juntar-se um modo debug com o centro configurável |
| Como testar com NIF? | Validar o formato e o dígito de controlo, e a unicidade no Firestore. Usar NIFs de teste válidos |
| Como validar se é munícipe? | Não há verificação real: o registo fica pendente e um operador ativa-o depois de ver o nome, a localização e o NIF |
| Língua: no aparelho, na app ou nos dois? | O enunciado diz "consoante configurado no dispositivo": `values/strings.xml` em inglês e `values-pt/strings.xml`, sem seletor na app |
| Permissões: tudo o que se usa precisa de gestão de permissões? | Sim, as permissões perigosas: localização (primeiro e segundo plano), câmara, notificações e, se for preciso, acesso a ficheiros de media. Tratar também a recusa |

### Ambiguidades e pontos difíceis a levar às aulas

- **MFA:** o MFA nativo do Firebase obriga ao Identity Platform (SMS ou TOTP) e ao plano Blaze. A alternativa é um código por e-mail feito por nós.
- **Lógica no servidor:** o push entre utilizadores, o fecho automático aos 5 dias e o aviso das 24 h ficam mais limpos com Cloud Functions, que também exigem o plano Blaze.
- **Geofence de 20 m:** a API de Geofencing do Android não é fiável abaixo de cerca de 100 m. É mais seguro calcular a distância a partir da localização do operador.
- **"Pr. República":** presume-se a de Coimbra.
- **Reabertura:** o enunciado não diz para que estado volta o ticket (Aberto ou Em Resolução).
- **Domínio de e-mail dos operadores:** não está definido; tornar configurável.

## 5. Documentação criada

| Ficheiro | Para quê |
|---|---|
| `docs/PROGRESSO_CITYFIX.md` | Estado geral: caixa "Onde estamos", lembretes, regras para trabalhar a dois e todos os requisitos do enunciado com estado, quem, data e notas |
| `docs/sessoes/SESSAO_01_2026-10-26_enunciado-e-projeto-inicial.md` | Este ficheiro |

### Convenção para os próximos registos

- Um ficheiro por sessão de trabalho, em `docs/sessoes/`.
- Nome: `SESSAO_<número com 2 dígitos>_<data AAAA-MM-DD>_<assunto-curto>.md`, por exemplo `SESSAO_02_2026-10-29_firebase-e-navegacao.md`.
- Conteúdo: data, quem, o que foi feito, decisões, problemas encontrados e o que ficou por fazer.
- No fim de cada sessão, atualizar também o `PROGRESSO_CITYFIX.md`.

## Por fazer no fim da sessão

- Assistir às aulas sobre a forma de fazer o projeto e reorganizar o `PROGRESSO_CITYFIX.md` se houver um guia.
- Levar as dúvidas da secção 4 ao professor.
- Commit da pasta `docs/` (o Claude não fez commit).
