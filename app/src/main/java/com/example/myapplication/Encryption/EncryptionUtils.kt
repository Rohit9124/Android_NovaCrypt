package com.example.myapplication.Encryption

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.content.Context
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.crypto.CipherOutputStream
import javax.crypto.CipherInputStream

object EncryptionUtils {

    // -----------------------------
    // Common AES constants
    // -----------------------------
    private const val AES_MODE_GCM = "AES/GCM/NoPadding"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val SALT_LEN = 16
    private const val NONCE_LEN = 12
    private const val TAG_LEN = 128 // in bits
    private const val ALGORITHM = "AES"

    // -----------------------------
    // PASSWORD-BASED TEXT ENCRYPTION / DECRYPTION
    // -----------------------------
    fun encryptText(plainText: String, password: String): String {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val nonce = ByteArray(NONCE_LEN).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(AES_MODE_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LEN, nonce))
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charset.forName("UTF-8")))

        val out = ByteArray(salt.size + nonce.size + cipherBytes.size)
        System.arraycopy(salt, 0, out, 0, salt.size)
        System.arraycopy(nonce, 0, out, salt.size, nonce.size)
        System.arraycopy(cipherBytes, 0, out, salt.size + nonce.size, cipherBytes.size)

        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    fun decryptText(cipherBase64: String, password: String): String {
        val all = Base64.decode(cipherBase64, Base64.NO_WRAP)
        require(all.size >= (SALT_LEN + NONCE_LEN + 1)) { "Cipher text too short" }

        val salt = all.copyOfRange(0, SALT_LEN)
        val nonce = all.copyOfRange(SALT_LEN, SALT_LEN + NONCE_LEN)
        val cipherBytes = all.copyOfRange(SALT_LEN + NONCE_LEN, all.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(AES_MODE_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LEN, nonce))
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charset.forName("UTF-8"))
    }

    // -----------------------------
    // PASSWORD-BASED FILE ENCRYPTION / DECRYPTION
    // -----------------------------
    fun encryptFile(inputFile: File, outputFile: File, password: String) {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val nonce = ByteArray(NONCE_LEN).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance(AES_MODE_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LEN, nonce))

        val fileBytes = FileInputStream(inputFile).use { it.readBytes() }
        val encryptedBytes = cipher.doFinal(fileBytes)

        FileOutputStream(outputFile).use {
            it.write(salt)
            it.write(nonce)
            it.write(encryptedBytes)
        }
    }

    fun decryptFile(inputFile: File, outputFile: File, password: String) {
        val fileBytes = FileInputStream(inputFile).use { it.readBytes() }
        require(fileBytes.size >= (SALT_LEN + NONCE_LEN + 1)) { "File too short" }

        val salt = fileBytes.copyOfRange(0, SALT_LEN)
        val nonce = fileBytes.copyOfRange(SALT_LEN, SALT_LEN + NONCE_LEN)
        val cipherBytes = fileBytes.copyOfRange(SALT_LEN + NONCE_LEN, fileBytes.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(AES_MODE_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LEN, nonce))
        val decryptedBytes = cipher.doFinal(cipherBytes)

        FileOutputStream(outputFile).use { it.write(decryptedBytes) }
    }


    fun encryptFile(fileBytes: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE_GCM) // switched to GCM for authentication
        val nonce = ByteArray(NONCE_LEN).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LEN, nonce))
        val encrypted = cipher.doFinal(fileBytes)
        return nonce + encrypted // prepend nonce for decryption
    }

    fun decryptFile(fileBytes: ByteArray, secretKey: SecretKey): ByteArray {
        require(fileBytes.size > NONCE_LEN) { "Encrypted data too short" }
        val nonce = fileBytes.copyOfRange(0, NONCE_LEN)
        val cipherData = fileBytes.copyOfRange(NONCE_LEN, fileBytes.size)

        val cipher = Cipher.getInstance(AES_MODE_GCM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LEN, nonce))
        return cipher.doFinal(cipherData)
    }

    suspend fun encryptImage(
        context: Context,
        bitmap: Bitmap,
        secretKey: SecretKey
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // Write bitmap temporarily to cache (avoid large in-memory array)
            val tempInput = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempInput).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            // Encrypt the file using streaming
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, nonce))

            val byteOutput = ByteArrayOutputStream()
            byteOutput.write(nonce) // prepend nonce first

            CipherOutputStream(byteOutput, cipher).use { cos ->
                FileInputStream(tempInput).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        cos.write(buffer, 0, bytesRead)
                    }
                }
            }

            tempInput.delete()
            byteOutput.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun decryptImage(
        encryptedBytes: ByteArray,
        secretKey: SecretKey
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            require(encryptedBytes.size > 12) { "Invalid data" }
            val nonce = encryptedBytes.copyOfRange(0, 12)
            val cipherData = encryptedBytes.copyOfRange(12, encryptedBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, nonce))

            // Stream decrypt into memory (safe even for large images)
            val inputStream = CipherInputStream(cipherData.inputStream(), cipher)
            val decryptedBytes = inputStream.readBytes()

            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    // -----------------------------
    // KEY HELPERS
    // -----------------------------
    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(KEY_LENGTH)
        return keyGen.generateKey()
    }

    fun keyToString(secretKey: SecretKey): String {
        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    fun stringToKey(keyString: String): SecretKey {
        val decoded = Base64.decode(keyString, Base64.NO_WRAP)
        return SecretKeySpec(decoded, ALGORITHM)
    }

    // -----------------------------
    // HELPER
    // -----------------------------
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, ALGORITHM)
    }
}
