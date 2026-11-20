package fr.m2ipm.clinique.controller;

import fr.m2ipm.clinique.service.ConflitDeCreneauException;
import fr.m2ipm.clinique.service.OperationInvalideException;
import fr.m2ipm.clinique.service.RessourceIntrouvableException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduit les erreurs métier en réponses HTTP lisibles (champ « detail » en français). */
@RestControllerAdvice(assignableTypes = {ApiController.class, ChatController.class})
public class ApiExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    ProblemDetail introuvable(RessourceIntrouvableException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflitDeCreneauException.class)
    ProblemDetail conflit(ConflitDeCreneauException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(OperationInvalideException.class)
    ProblemDetail invalide(OperationInvalideException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }
}
