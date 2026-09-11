#!/usr/bin/env bash
# Lança uma versão: compila assinado, e publica no GitHub Releases.
#
# O IzzyOnDroid vai buscar o APK aqui automaticamente a cada versão nova, por
# isso a etiqueta e o ficheiro têm de seguir sempre o mesmo formato. Quem usa
# Obtainium também lê daqui.
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f keystore.properties ]; then
  echo "Falta o keystore.properties. Cria a chave primeiro:"
  echo
  echo "  keytool -genkeypair -v -keystore bruma.jks \\"
  echo "    -keyalg RSA -keysize 4096 -validity 10000 -alias bruma"
  echo
  echo "  printf 'storeFile=bruma.jks\\nstorePassword=...\\nkeyAlias=bruma\\nkeyPassword=...\\n' > keystore.properties"
  echo "  chmod 600 keystore.properties"
  exit 1
fi

# Uma etiqueta tem de apontar para um commit que exista. Etiquetar com
# alterações por guardar dá uma versão que ninguém consegue reproduzir a partir
# do repositório — que é precisamente o que o IzzyOnDroid e o F-Droid pedem.
if [ -n "$(git status --porcelain)" ]; then
  echo "Há alterações por commitar. Guarda-as antes de lançar:"
  git status --short
  exit 1
fi

VERSAO=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
CODIGO=$(grep -oP 'versionCode = \K[0-9]+' app/build.gradle.kts)
echo "A lançar a v$VERSAO (versionCode $CODIGO)"

if git rev-parse "v$VERSAO" >/dev/null 2>&1; then
  echo "A etiqueta v$VERSAO já existe. Sobe o versionCode e o versionName primeiro."
  exit 1
fi

SAIDA=app/build/outputs/apk/release
SAIDA_TESTE="$SAIDA/app-arm64-v8a-release.apk"

./gradlew :app:assembleRelease

# Confirmar que saiu mesmo assinado: um APK sem assinatura instala em lado
# nenhum, e o erro só apareceria no telemóvel de quem o tentasse instalar.
ALINHADOR=$(ls -d "${ANDROID_HOME:-$HOME/android-sdk}"/build-tools/* | sort -V | tail -1)
if ! "$ALINHADOR/apksigner" verify "$SAIDA_TESTE" >/dev/null 2>&1; then
  echo "O APK saiu sem assinatura válida. Verifica o keystore.properties."
  exit 1
fi

# Os APKs por arquitetura são os que interessam: o universal leva quatro motores
# dos quais o telemóvel só usa um, e passa dos 300 MB.
APKS=$(ls "$SAIDA"/app-*-release.apk | grep -v universal)
echo "Ficheiros:"; for f in $APKS; do printf "  %6s  %s\n" "$(du -h "$f" | cut -f1)" "$(basename "$f")"; done

NOTAS=$(cat fastlane/metadata/android/en-US/changelogs/"$CODIGO".txt 2>/dev/null || echo "Ver os commits.")

git tag -a "v$VERSAO" -m "Bruma v$VERSAO"
git push origin "v$VERSAO"
gh release create "v$VERSAO" $APKS --title "Bruma v$VERSAO" --notes "$NOTAS"
echo "Lançada: $(gh release view "v$VERSAO" --json url --jq .url)"
