package bd.bracuinsiders.web;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.dao.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class FormErrors {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView tooLarge(HttpServletResponse response) {
        return error(
                response,
                413,
                "File too large",
                "Each file can be up to 4 MB. A post can contain up to six images.");
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        OptimisticLockingFailureException.class
    })
    public ModelAndView conflict(HttpServletResponse response) {
        return error(
                response,
                409,
                "This item changed",
                "Reopen the item and check its current values before trying again.");
    }

    private ModelAndView error(HttpServletResponse res, int status, String title, String detail) {
        res.setStatus(status);
        return new ModelAndView("error")
                .addObject("status", status)
                .addObject("heading", title)
                .addObject("detail", detail);
    }
}
