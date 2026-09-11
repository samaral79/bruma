# Bruma

Navegador Android para uma mão, com Tor embutido e uBlock Origin a sério.

Construído sobre **GeckoView** — o motor do Firefox, o mesmo que o Tor Browser e
o Mull usam — e não sobre o WebView do sistema. A diferença não é de gosto: o
WebView não corre extensões, não expõe `resistFingerprinting`, não isola por
primeira parte, e o proxy dele aplica-se ao processo inteiro. Nada disso se
contorna por fora.

## O que faz

- **Tor dentro da app.** O binário do tor vem no APK (kmp-tor 2.6.0 +
  resource-exec-tor 409.5.0, que é o tor 0.4.9.5) e corre num serviço em
  primeiro plano. Não precisa do Orbot nem de nenhuma outra app.
- **`.onion` a funcionar.** O Gecko sai por `socks5://127.0.0.1:<porto>` com
  `network.proxy.socks_remote_dns` ligado e `network.dns.blockDotOnion`
  desligado — as duas preferências sem as quais um endereço onion nem chega a
  ser tentado. Escrever um `.onion` com o tor desligado liga-o e abre a página
  sozinho quando houver circuito.
- **DuckDuckGo por omissão**, e com o tor ligado troca sozinho para o
  [serviço onion oficial](https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/),
  para a pesquisa nunca sair da rede tor por um nó de saída.
- **uBlock Origin 1.74.0**, a extensão verdadeira, embutida nos assets e
  instalada por `ensureBuiltIn`. Bloqueia desde o primeiro pedido e não depende
  de a Mozilla estar de pé. Autorizado também em separadores privados.
- **Proteção do Gecko por cima**: ETP estrito, isolamento de cookies por sítio,
  proteção contra rastreio por redireccionamento, remoção de parâmetros de
  seguimento (`?fbclid=`, `?gclid=`), WebRTC desligado, referenciador só dentro
  da mesma origem, antecipação de DNS e de ligações desligada.
- **Uma mão a sério.** Nenhum controlo fora do alcance do polegar.

## A interface

Não há barra de topo nem de baixo. Há uma **cápsula na lateral**, do lado da mão
dominante e a 62% da altura — onde o polegar assenta com o telemóvel seguro pela
base. A cápsula mostra tudo o que é preciso saber num ponto colorido (tor, cifra,
onion) e o número de separadores.

| Gesto na cápsula     | O que faz                        |
|----------------------|----------------------------------|
| tocar                | escrever endereço ou pesquisa    |
| arrastar ↑ / ↓       | separador seguinte / anterior    |
| arrastar para dentro | coluna de ações                  |
| arrastar para fora   | esconder a cápsula               |
| premir e segurar     | coluna de ações                  |

A página inicial tem duas coisas: um fundo e uma caixa para escrever. O fundo por
omissão é desenhado — um degradê de névoa, que é o que o nome quer dizer — e pode
ser trocado por uma fotografia nas Definições (segundo toque volta à névoa).

Não há painel de sítios mais visitados de propósito: é um histórico exposto a
quem pegar no telemóvel, o contrário do que esta app existe para fazer.

## O que **não** faz

- **Não é o Tor Browser.** O `resistFingerprinting` está nas Definições e ajuda
  muito, mas não substitui os anos de trabalho de uniformização do Tor Browser.
- **Não tem histórico, marcadores nem sincronização.** Deliberado nesta versão.
- **O modo tor é global, não por separador.** Um "separador tor" ao lado de
  separadores diretos daria a impressão de isolamento sem o haver.
- **Não segue ligações para outras apps** (`intent://`, `market://`, `tg://`):
  sairiam do tor sem aviso e revelariam que apps estão instaladas.

## Armadilhas que custaram a descobrir

Ficam registadas porque nenhuma delas dá um erro que se perceba.

- **`Application.onCreate()` corre em todos os processos da app.** O GeckoView
  cria vários (`:gpu`, `:tab`, `:crashhelper`, um por separador isolado), e em
  cada um deles o `onCreate` voltava a chamar `GeckoRuntime.create()`. Cada filho
  tentava ser um motor completo, chegava a `JNI_READY`, abortava — e o processo
  principal morria com SIGKILL uns seis segundos depois do arranque. Sem exceção
  em Java, sem minidump, sem `am_kill` do ActivityManager. O que denunciou foi o
  `am_proc_died` aparecer **sem nenhum `am_kill` antes**: ninguém no sistema o
  tinha mandado morrer. A guarda está em `BrumaApp.ehProcessoPrincipal()`.
- **O aapt descarta diretórios começados por `_`** (padrão `<dir>_*`). O uBlock
  guarda as traduções em `_locales/`, que nunca chegavam ao APK; o Gecko
  rejeitava a extensão com um lacónico "Extension is invalid" e o erro verdadeiro
  (`NS_ERROR_FILE_NOT_FOUND` em `_locales/en/messages.json`) só aparecia no
  `GeckoConsole`. Resolve-se com `androidResources { ignoreAssetsPatterns }`.
- **O Firefox 155 já traz o Manifest V2 desligado.** O uBO é V2, por isso
  `extensions.manifestV2.enabled` tem de ser posta **antes** de instalar, e a
  instalação tem de esperar pela confirmação.
- **Declarar `FontFamily.SansSerif` não chega.** Em telemóveis Samsung o
  `sans-serif` é remapeado para a fonte que o dono escolheu no sistema — no
  aparelho de teste, uma manuscrita. A Inter vai embutida em `res/font`.
- **O GeckoView 155 exige compilar contra android-37.1**, não 37.0
  (`compileSdk = 37` + `compileSdkMinor = 1`).
- **O README do kmp-tor-resource está desatualizado**: manda pôr
  `android.bundle.enableUncompressedNativeLibs=false` no `gradle.properties`, que
  foi removido no AGP 8.1 e faz o AGP 9 recusar configurar o projeto. O
  equivalente é `packaging { jniLibs.useLegacyPackaging = true }`.
- **O porto SOCKS do tor é pedido em `auto` e muda a cada arranque**, por isso o
  proxy do Gecko tem de ser reescrito sempre que o tor fica pronto.
- Entre carregar no interruptor do tor e haver circuito há segundos em que a app
  diria "tor ligado" e sairia em direto. Nesse intervalo a rede é apontada a um
  porto fechado: os pedidos falham em vez de vazarem.

## Compilar

```bash
./gradlew :app:assembleDebug
```

Há divisão por ABI; para o telemóvel chega o `arm64-v8a` (~128 MB, quase tudo
GeckoView e tor).

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

## Estado

Testado num Galaxy A56 (Android 16, API 36): arranca, o uBlock instala, o tor
faz circuito e o `.onion` do DuckDuckGo abre. Por afinar: o aspeto e os gestos,
que só o uso diário dirá.

## Licenças

O uBlock Origin é GPLv3 e está redistribuído sem modificações em
`app/src/main/assets/extensions/ublock/` (retirada apenas a pasta `META-INF/`
com a assinatura, que não valida fora do `.xpi`). O GeckoView é MPL-2.0, o
kmp-tor Apache-2.0, o tor BSD de 3 cláusulas, e a Inter é SIL OFL 1.1.
