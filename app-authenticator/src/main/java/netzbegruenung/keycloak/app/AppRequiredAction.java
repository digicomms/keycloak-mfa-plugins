package netzbegruenung.keycloak.app;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import netzbegruenung.keycloak.app.actiontoken.ActionTokenUtil;
import netzbegruenung.keycloak.app.actiontoken.AppSetupActionToken;
import org.jboss.logging.Logger;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Base64;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

public class AppRequiredAction implements RequiredActionProvider, CredentialRegistrator {

	private static final Logger logger = Logger.getLogger(AppRequiredAction.class);

	public static String PROVIDER_ID = "app-register";

	@Override
	public InitiatedActionSupport initiatedActionSupport() {
		return InitiatedActionSupport.SUPPORTED;
	}

	@Override
	public void evaluateTriggers(RequiredActionContext requiredActionContext) {

	}

	@Override
	public void requiredActionChallenge(RequiredActionContext context) {
		URI actionTokenUrl = ActionTokenUtil.createActionToken(
			AppSetupActionToken.class,
			context.getAuthenticationSession(),
			context.getSession(),
			context.getRealm(),
			context.getUser(),
			context.getUriInfo()
		);

		Response challenge = context.form()
			.setAttribute("appAuthQrCode", createQrCode(actionTokenUrl))
			.setAttribute("appAuthActionTokenUrl", actionTokenUrl.toString())
			.createForm("app-auth-setup.ftl");
		context.challenge(challenge);
	}

	private String createQrCode(URI uri) {
		try {
			int width = 400;
			int height = 400;

			QRCodeWriter writer = new QRCodeWriter();
			final BitMatrix bitMatrix = writer.encode(
				ActionTokenUtil.uriToJson(uri).toString(),
				BarcodeFormat.QR_CODE,
				width,
				height
			);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
			bos.close();

			return Base64.encodeBytes(bos.toByteArray());
		} catch (WriterException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void processAction(RequiredActionContext context) {
		final AuthenticationSessionModel authSession = context.getAuthenticationSession();

		if (!Boolean.parseBoolean(authSession.getAuthNote("appSetupSuccessful"))) {
			URI actionTokenUrl = ActionTokenUtil.createActionToken(
				AppSetupActionToken.class,
				context.getAuthenticationSession(),
				context.getSession(),
				context.getRealm(),
				context.getUser(),
				context.getUriInfo()
			);

			Response challenge = context.form()
				.setAttribute("appAuthQrCode", createQrCode(actionTokenUrl))
				.setAttribute("appAuthActionTokenUrl", actionTokenUrl.toString())
				.setError("appAuthSetupError")
				.createForm("app-auth-setup.ftl");
			context.challenge(challenge);
			return;
		}
		context.success();
	}

	@Override
	public void close() {

	}
}
