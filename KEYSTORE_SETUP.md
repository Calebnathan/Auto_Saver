# Keystore Setup Guide

This guide explains how to set up code signing for your Android app, both locally and in GitHub Actions.

## Overview

The build system now supports **optional signing**:
- ✅ **With keystore**: Builds signed APK/AAB (production-ready)
- ✅ **Without keystore**: Builds unsigned APK/AAB (for testing)

The workflow will automatically detect if a keystore is available and build accordingly.

---

## Local Development Setup

### 1. Generate a Keystore (One-time Setup)

If you don't have a keystore yet, create one:

```bash
keytool -genkey -v -keystore release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

You'll be prompted to enter:
- **Keystore password**: Choose a strong password
- **Key password**: Can be the same as keystore password
- **Your information**: Name, organization, location, etc.

**⚠️ IMPORTANT**: Store this keystore file and passwords securely! You'll need them to update your app on Google Play.

### 2. Create `keystore.properties`

Create a file called `keystore.properties` in your project root:

```properties
storeFile=release.keystore
storePassword=YOUR_KEYSTORE_PASSWORD
keyAlias=release
keyPassword=YOUR_KEY_PASSWORD
```

**⚠️ NEVER COMMIT THIS FILE!** It's already in `.gitignore`.

### 3. Local Build Commands

**Build signed release APK:**
```bash
./gradlew assembleRelease
```

**Build signed release AAB (for Play Store):**
```bash
./gradlew bundleRelease
```

Output locations:
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

---

## GitHub Actions Setup

### 1. Encode Your Keystore

Convert your keystore to base64:

```bash
base64 -i release.keystore | tr -d '\n' > keystore_base64.txt
```

This creates a `keystore_base64.txt` file containing the encoded keystore.

### 2. Add GitHub Secrets

Go to your repository settings:
1. Navigate to **Settings → Secrets and variables → Actions**
2. Click **"New repository secret"**
3. Add the following secrets:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `KEYSTORE_BASE64` | Content of `keystore_base64.txt` | Base64-encoded keystore file |
| `KEYSTORE_PASSWORD` | Your keystore password | Password you used when creating keystore |
| `KEY_ALIAS` | `release` | The alias (usually "release") |
| `KEY_PASSWORD` | Your key password | Often same as keystore password |

### 3. How It Works

When you trigger the **Release Build** workflow:

**If keystore secrets exist:**
- ✅ Decodes the keystore
- ✅ Builds **signed** APK and AAB
- ✅ Ready for Google Play Store

**If keystore secrets missing:**
- ⚠️ Skips keystore decoding
- ⚠️ Builds **unsigned** APK and AAB
- ⚠️ For testing only (not installable on most devices)

---

## Building Without Signing (Optional)

If you want to build unsigned releases locally (for testing):

1. Remove or rename `keystore.properties`
2. Run the build commands as normal
3. The build will succeed with unsigned artifacts

---

## Security Best Practices

### ✅ DO:
- Keep `keystore.properties` in `.gitignore`
- Store keystore file securely (backup to encrypted storage)
- Use strong passwords for keystore and key
- Keep GitHub secrets private and secure
- Regenerate keystore if compromised

### ❌ DON'T:
- Never commit keystore files to version control
- Never commit passwords or secrets
- Never share keystore or passwords publicly
- Never use the same keystore for multiple apps

---

## Troubleshooting

### Local Build Fails: "Keystore not found"

**Symptom**: Build fails with keystore errors

**Solution**: 
- Check `keystore.properties` exists in project root
- Verify `storeFile` path in `keystore.properties` is correct
- Ensure keystore file exists at specified path

### GitHub Actions Fails: "Signing config not found"

**Symptom**: Workflow fails during signing

**Solution**:
1. Verify all 4 secrets are set correctly in GitHub
2. Check `KEYSTORE_BASE64` contains the full base64 string (no line breaks)
3. Re-encode and re-upload keystore if needed

### "Unsigned APK cannot be installed"

**Symptom**: APK installs but shows as "unsigned" or won't install

**Solution**:
- This is expected for unsigned builds
- Either set up keystore for signed builds, or use debug builds for testing
- Unsigned releases are only for checking build success, not distribution

---

## Migration from Old Setup

If you had a previous signing configuration:

1. **Keep your existing keystore!** Don't generate a new one if you've already published to Play Store
2. Update `keystore.properties` to match the new format above
3. Add GitHub secrets as described
4. The build configuration will automatically detect and use your keystore

---

## Play Store Publishing

To publish to Google Play Store, you **must** have a signed AAB:

1. Set up keystore (as described above)
2. Build release AAB: `./gradlew bundleRelease`
3. Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console

**First-time publishers**: Google Play requires that all future updates use the same keystore. Keep it safe!

---

## Questions?

If you have issues:
1. Check that `.gitignore` includes `keystore.properties` and `*.keystore`
2. Verify file paths are relative to project root
3. Test local builds first before setting up CI/CD
4. Review GitHub Actions logs for specific error messages