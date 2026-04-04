package com.example.alarm

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {
    // Thay thế bằng email và mật khẩu ứng dụng của bạn
    private const val SENDER_EMAIL = "caoalamr@gmail.com"
    private const val SENDER_PASSWORD = "xxvr sxxw sfvc vatq"

    fun sendVerificationCode(recipientEmail: String, code: String, callback: (Boolean) -> Unit) {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
            }
        })

        Thread {
            try {
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(SENDER_EMAIL))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                message.subject = "Mã xác nhận quên mật khẩu"
                message.setText("Mã xác nhận của bạn là: $code. Vui lòng không chia sẻ mã này cho bất kỳ ai.")

                Transport.send(message)
                callback(true)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }.start()
    }
}
