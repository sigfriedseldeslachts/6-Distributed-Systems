package be.uantwerpen.distributedclients.controllers;

import be.uantwerpen.distributedclients.services.InfoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("nodes")
public class NodeController {

    private InfoService infoservice;

    public NodeController(InfoService infoservice) {
        this.infoservice = infoservice;
    }

    @GetMapping
    public Object ShowDebug() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("amountOfNodes", infoservice.getAmountOfNodes());
        map.put("Node", infoservice.getNode());
        map.put("previousID", infoservice.getPreviousID());
        map.put("nextID", infoservice.getNextID());

        return map;
    }

    @PostMapping("next")
    public void CurrentAndNext(@RequestParam int currentID, @RequestParam int nextID) {
        this.infoservice.setPreviousID(currentID);
        this.infoservice.setNextID(nextID);
    }
    @PutMapping("next")
    public void Next(@RequestParam int nextID) {
        this.infoservice.setNextID(nextID);
    }

    @PostMapping("previous")
    public void CurrentAndPrevious(@RequestParam int currentID, @RequestParam int previousID) {
        this.infoservice.setNextID(currentID);
        this.infoservice.setPreviousID(previousID);
    }
    @PutMapping("previous")
    public void Previous(@RequestParam int previousID) {
        this.infoservice.setPreviousID(previousID);
    }

    @PostMapping("{amount}")
    public void AmountExistingNodes(@PathVariable int amount, HttpServletRequest request) {
        this.infoservice.setNamingserverAddress(request.getRemoteAddr());
        this.infoservice.setAmountOfNodes(amount);
    }
}
