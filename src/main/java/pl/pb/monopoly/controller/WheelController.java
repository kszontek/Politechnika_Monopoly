package pl.pb.monopoly.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.pb.monopoly.dto.WheelResultDto;
import pl.pb.monopoly.service.WheelService;

// Codzienne Kolo Fortuny - strona z kolem i sam los. Limit "raz na dzien" pilnuje WheelService.
@Controller
public class WheelController {

    private final WheelService wheelService;

    public WheelController(WheelService wheelService) {
        this.wheelService = wheelService;
    }

    // strona kola - canSpin mowi frontowi czy aktywowac przycisk czy pokazac "wroc jutro"
    @GetMapping("/wheel")
    public String wheelPage(Authentication authentication, Model model) {
        model.addAttribute("segments", WheelService.rewardLabels());
        model.addAttribute("canSpin", wheelService.canSpinToday(authentication.getName()));
        return "wheel";
    }

    // samo zakrecenie - wynik losuje backend, front tylko odgrywa animacje pod ten wynik
    @PostMapping("/wheel/spin")
    @ResponseBody
    public ResponseEntity<WheelResultDto> spin(Authentication authentication) {
        return ResponseEntity.ok(wheelService.spin(authentication.getName()));
    }
}
