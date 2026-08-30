package perez.gustavo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.util.Properties;

public class NotificadorApp {

    private static final String SLACK_WEBHOOK_URL = "url-do-slack";
    private static final String EMAIL_REMETENTE = "email-que-foi-configurado-p-enviar-os-emails";
    private static final String SENHA_APP = "senha-de-app-gerada-no-email";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/notificar", exchange -> {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
                    JsonObject dados = new Gson().fromJson(isr, JsonObject.class);

                    String mensagem = dados.get("mensagem").getAsString();
                    boolean enviarSlack = dados.get("enviarSlack").getAsBoolean();
                    boolean enviarEmail = dados.get("enviarEmail").getAsBoolean();

                    if (enviarSlack) enviarParaSlack(mensagem);

                    if (enviarEmail) {
                        String emailDestino = dados.get("emailDestinatario").getAsString();
                        enviarParaEmail(emailDestino, mensagem);
                    }

                    String resposta = "{\"status\": \"sucesso\"}";
                    exchange.sendResponseHeaders(200, resposta.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(resposta.getBytes());
                    os.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Servidor rodando em http://localhost:8080");
    }

    private static void enviarParaSlack(String mensagem) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("text", mensagem);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SLACK_WEBHOOK_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Slack enviado!");
    }

    private static void enviarParaEmail(String destinatario, String mensagemTexto) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_REMETENTE, SENHA_APP);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EMAIL_REMETENTE));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        message.setSubject("Nova Notificação (Projeto de Teste)");
        message.setText(mensagemTexto);

        Transport.send(message);
        System.out.println("E-mail enviado para " + destinatario);
    }
}