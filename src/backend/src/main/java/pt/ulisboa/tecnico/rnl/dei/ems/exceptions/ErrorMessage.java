package pt.ulisboa.tecnico.rnl.dei.ems.exceptions;

public enum ErrorMessage {

	NO_SUCH_PERSON("Não existe nenhuma pessoa com o ID %s", 1001),
	PERSON_NAME_NOT_VALID("O nome da pessoa especificado não é válido.", 1002),
	PERSON_ALREADY_EXISTS("Já existe uma pessoa com o ID %s", 1003),
	PERSON_EMAIL_NOT_VALID("O email especificado não é válido.", 1004),
	PERSON_PASSWORD_NOT_VALID("A palavra-passe especificada não é válida.", 1005),
	EMAIL_ALREADY_EXISTS("Já existe uma conta com o email %s", 1006),
	SUBJECT_ALREADY_EXISTS("Já existe uma disciplina com o código %s", 1007),
	SCHOOL_ALREADY_EXISTS("Já existe uma escola com o código %s", 1008),
	NO_SUCH_SUBJECT("Não existe nenhuma disciplina com o ID %s", 1009),
	NO_SUCH_SCHOOL("Não existe nenhuma escola com o ID %s", 1010),
	STUDENT_MUST_HAVE_SCHOOL("O aluno deve ter uma escola", 1011),
	SCHOOL_STAFF_MUST_HAVE_SCHOOL("O funcionário deve ter uma escola", 1012),
	INVALID_PERSON_TYPE("O tipo de pessoa especificado não é válido.", 1013),
	TEACHER_MUST_HAVE_SCHOOL("O professor deve ter uma escola", 1014),
	TEACHER_MUST_HAVE_SUBJECT("O professor deve ter uma disciplina", 1015),
	NO_SUCH_EXAM("Não existe nenhum exame com o id %s", 1016),
	EXAM_NOT_PDF("O ficheiro do exame deve ser um PDF.", 1017),
	EXAM_STUDENT_WRONG_SCHOOL("O aluno não pertence à escola do funcionário.", 1018),
	NOT_STUDENT("A pessoa com o id %s não é um aluno.", 1019),
	EXAM_FILE_EMPTY("O ficheiro do exame está vazio.", 1020),
	EXAM_STORAGE_FAILED("Não foi possível guardar o ficheiro do exame.", 1021),
	NO_SUCH_QUESTION("Não existe nenhuma pergunta com o id %s", 1022),
	EXAM_FILE_MISSING("O ficheiro do exame não foi encontrado.", 1023),
	EXAM_NOT_UPLOADED("O exame tem de estar no estado UPLOADED para ser segmentado.", 1024),
	EXAM_SEGMENT_INVALID("As imagens e cotações da segmentação são inválidas.", 1025),
	QUESTION_STORAGE_FAILED("Não foi possível guardar a imagem da pergunta.", 1026),
	NO_TEACHERS_FOR_SUBJECT("Não existem professores para a disciplina %s.", 1027),
	EXAM_ALREADY_DISTRIBUTED("Existem exames LOCKED que já têm atribuições de correção.", 1028),
	EXAM_SUBMISSIONS_LOCKED("As submissões de exames estão fechadas. Não é possível carregar novos exames.", 1029),
	EXAM_NOT_ALL_LOCKED("A distribuição só é permitida quando não há exames UPLOADED ou SEGMENTED.", 1030),
	NO_SUCH_EVALUATE("Não existe nenhuma atribuição de correção com o id %s", 1031),
	EVALUATE_NOT_GRADABLE("A atribuição de correção não está num estado que permita classificar.", 1032),
	EVALUATE_GRADE_INVALID("A nota deve estar entre 0 e a cotação máxima (%s).", 1033),
	QUESTION_FILE_MISSING("A imagem da pergunta não foi encontrada.", 1034),
	EXAM_NOT_GRADED_FOR_RELEASE(
			"O exame tem de ter as notas publicadas (GRADES_PUBLISHED) para ser disponibilizado.",
			1035),
	EXAM_RELEASE_FILTERS_REQUIRED("É necessário indicar a escola e a disciplina para disponibilizar em lote.", 1036),
	EXAM_NOT_OPEN_FOR_CONSULTATION("A prova ainda não está disponível para consulta.", 1037),
	EXAM_NOT_PUBLISHED_FOR_RELEASE("O exame tem de ter as notas publicadas para disponibilizar a prova.", 1038),
	EXAM_PUBLISH_SCHOOL_REQUIRED("É necessário indicar a escola para publicar as notas.", 1039),
	EXAM_PUBLISH_SUBJECT_REQUIRED("É necessário indicar a disciplina para publicar as notas.", 1040),
	EXAM_PUBLISH_NOT_ALL_GRADED(
			"Só é possível publicar notas quando todos os exames da escola e disciplina estão corrigidos.",
			1041),
	EXAM_CONSULTATION_REQUEST_INVALID(
			"Só é possível pedir a prova quando as notas estão publicadas e a prova ainda não foi disponibilizada.",
			1042),
	REVIEW_REQUEST_NOT_ALLOWED(
			"Só é possível pedir revisão com a prova disponibilizada e dentro do prazo.",
			1043),
	REVIEW_REQUEST_ALREADY_EXISTS("Já existe um pedido de revisão para esta pergunta.", 1044),
	REVIEW_JUSTIFICATION_REQUIRED("A justificação do pedido de revisão é obrigatória.", 1045),
	NO_SUCH_QUESTION_ON_EXAM("Não existe a pergunta %s neste exame.", 1046),
	REQUEST_BODY_INVALID("O pedido enviado é inválido.", 1047),
	SCHOOL_FIELDS_NOT_VALID("O nome, código e região da escola são obrigatórios.", 1048),
	SUBJECT_FIELDS_NOT_VALID("O nome e código da disciplina são obrigatórios.", 1049),

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
