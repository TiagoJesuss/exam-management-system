package pt.ulisboa.tecnico.rnl.dei.ems.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DEIException extends RuntimeException {
	private final ErrorMessage errorMessage;
	private static final Logger logger = LoggerFactory.getLogger(DEIException.class);

	public DEIException(ErrorMessage errorMessage) {
		super(errorMessage.getLabel());
		logger.error(errorMessage.getLabel());
		this.errorMessage = errorMessage;
	}

	public DEIException(ErrorMessage errorMessage, String value) {
		super(String.format(errorMessage.getLabel(), value));
		logger.error(errorMessage.getLabel(), value);
		this.errorMessage = errorMessage;
	}

	public ErrorMessage getErrorMessage() {
		return this.errorMessage;
	}

	public int getCode() {
		return this.errorMessage.getCode();
	}
}
