$(document).ready(function() {
	$("#login-form").submit(function() {
		if(!$('#username').val() || !$("#password").val()) {
			return false;
		}
		var encrypt = new JSEncrypt();
		encrypt.setPublicKey($("#encrypt_key").val());
		var encrypted = encrypt.encrypt($("#password").val());
		$("#password_encrypted").val(encrypted);
		console.log(encrypted);
		return true;
	});
});