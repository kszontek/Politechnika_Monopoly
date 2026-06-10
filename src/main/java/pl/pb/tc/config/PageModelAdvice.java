package pl.pb.tc.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PageModelAdvice {

    @ModelAttribute("activePage")
    public String activePage(HttpServletRequest request) {
        String path = request.getServletPath();
        if ("/login".equals(path)) return "login";
        if ("/register".equals(path)) return "register";
        if ("/dashboard".equals(path)) return "dashboard";
        if (path.startsWith("/users")) return "users";
        if ("/".equals(path)) return "home";
        return "";
    }
}
