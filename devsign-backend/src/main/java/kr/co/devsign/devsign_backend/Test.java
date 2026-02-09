package kr.co.devsign.devsign_backend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class Test {
    @GetMapping("/test")
    @ResponseBody
    public String printTest(){
        return "Test Success";
    }
}
