package pt.ulisboa.tecnico.rnl.dei.ems.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {
	private static Logger logger = LoggerFactory.getLogger(CustomExceptionHandler.class);

	@ExceptionHandler(DEIException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public DEIExceptionDto DEIException(DEIException e) {
		return new DEIExceptionDto(e);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		return ResponseEntity.badRequest()
				.body(new DEIExceptionDto(new DEIException(ErrorMessage.REQUEST_BODY_INVALID)));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		return ResponseEntity.badRequest()
				.body(new DEIExceptionDto(new DEIException(ErrorMessage.REQUEST_BODY_INVALID)));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public DEIExceptionDto illegalArgument(IllegalArgumentException e) {
		return new DEIExceptionDto(new DEIException(ErrorMessage.INVALID_PERSON_TYPE));
	}

	@ExceptionHandler(AuthenticationException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public DEIExceptionDto authenticationException(AuthenticationException e) {
		return new DEIExceptionDto(new DEIException(ErrorMessage.INVALID_CREDENTIALS));
	}

	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public DEIExceptionDto accessDenied(AccessDeniedException e) {
		return new DEIExceptionDto(new DEIException(ErrorMessage.ACCESS_DENIED));
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public DEIExceptionDto unexpectedException(Exception e) {
		logger.error(e.getMessage(), e);
		return new DEIExceptionDto(e);
	}
}
