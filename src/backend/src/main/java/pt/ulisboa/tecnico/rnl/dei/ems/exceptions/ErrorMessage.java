package pt.ulisboa.tecnico.rnl.dei.ems.exceptions;

public enum ErrorMessage {

	NO_SUCH_PERSON("Não existe nenhuma pessoa com o ID %s", 1001),
	PERSON_NAME_NOT_VALID("O nome da pessoa especificado não é válido.", 1002),
	PERSON_ALREADY_EXISTS("Já existe uma pessoa com o ID %s", 1003),
	PERSON_EMAIL_NOT_VALID("O email especificado não é válido.", 1004),
	PERSON_PASSWORD_NOT_VALID("A palavra-passe especificada não é válida.", 1005),
	EMAIL_ALREADY_EXISTS("Já existe uma conta com o email %s", 1006),

	INVALID_CREDENTIALS("Email ou palavra-passe inválidos.", 2001),
	CANNOT_IMPERSONATE_SELF("Não é possível personificar a própria conta.", 2002),
	NOT_AUTHENTICATED("Não autenticado.", 2003),
	NOT_IMPERSONATING("Não está a personificar nenhuma conta.", 2004),
	ACCESS_DENIED("Não tem permissões para executar esta ação.", 2005);

	private final String label;
	private final int code;

	ErrorMessage(String label, int code) {
		this.label = label;
		this.code = code;
	}

	public String getLabel() {
		return this.label;
	}

	public int getCode() {
		return this.code;
	}
}
