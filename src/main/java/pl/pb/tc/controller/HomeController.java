package pl.pb.tc.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.pb.tc.repository.UserRepository;
import pl.pb.tc.service.UserService;

@Controller
public class HomeController {

    private final UserService userService;
    private final UserRepository userRepository;

    public HomeController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("ranking", userService.topPlayers());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        var user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "dashboard";
    }
}
