<html>

<head>
<meta name='layout' content='main'/>
<title><g:message code='spring.security.ui.register.title' /></title>
</head>

<body>

	<p />

	<s2ui:form width='650' height='550' elementId='loginFormContainer'
		titleCode='spring.security.ui.register.description' center='true'>

		<g:form action='register' name='registerForm'>

			<g:if test='${emailSent}'>
				<br />
				<g:message code='spring.security.ui.register.sent' />
			</g:if>
			<g:else>

				<br />

				<table>
					<tbody>

						<s2ui:textFieldRow name='username' labelCode='user.username.label'
							bean="${command}" size='40' labelCodeDefault='Username'
							value="${command.username}" />

						<s2ui:textFieldRow name='email' bean="${command}"
							value="${command.email}" size='40' labelCode='user.email.label'
							labelCodeDefault='E-mail' />

						<s2ui:textFieldRow name='apellidoPaterno'
							labelCode='user.apellidoPaterno.label' bean="${command}"
							labelCodeDefault='Apellido Paterno'
							value="${command.apellidoPaterno}" />

						<s2ui:textFieldRow name='apellidoMaterno'
							labelCode='user.apellidoMaterno.label' bean="${command}"
							labelCodeDefault='Apellido Materno'
							value="${command.apellidoMaterno}" />

						<s2ui:textFieldRow name='primerNombre'
							labelCode='user.primerNombre.label' bean="${command}"
							labelCodeDefault='Primer Nombre' value="${command.primerNombre}" />

						<s2ui:textFieldRow name='segundoNombre'
							labelCode='user.segundoNombre.label' bean="${command}"
							labelCodeDefault='Segundo Nombre'
							value="${command.segundoNombre}" />

						<s2ui:passwordFieldRow name='password'
							labelCode='user.password.label' bean="${command}" size='40'
							labelCodeDefault='Password' value="${command.password}" />

						<s2ui:passwordFieldRow name='password2'
							labelCode='user.password2.label' bean="${command}" size='40'
							labelCodeDefault='Password (again)' value="${command.password2}" />

						<tr class="prop">
							<td valign="top" class="name"></td>

							<td valign="top" class="value"><br />

								<div>
									<g:if test='${captchaInvalido}'>
										<div style="color: red;">
											<g:message code='registro.captcha.volverIntentar'/>
										</div>
									</g:if>
								</div>

								<div>
									<img
										src="${createLink(controller: 'simpleCaptcha', action: 'captcha')}" />
								</div>
								<br />
								
								<div>
									<label for="captcha">Capture el texto de la imagen</label>
								</div>								
								
								<div>
									<g:textField name="captcha" />
								</div> <br /></td>
						</tr>
					</tbody>
				</table>

				<input type='submit' value='Crear cuenta de Usuario' id='create_submit' class='s2ui_hidden_button' />

			</g:else>

		</g:form>

	</s2ui:form>

	<script>
		$(document).ready(function() {
			$('#username').focus();
		});
	</script>

</body>
</html>
