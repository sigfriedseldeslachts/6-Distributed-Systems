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

        map.put("previousID", infoservice.getPreviousID());
        map.put("nextID", infoservice.getNextID());
        map.put("own_hash", infoservice.getSelfNode().hashCode());
        map.put("own_node", infoservice.getSelfNode());
        map.put("amountOfNodes", infoservice.getAmountOfNodes());

        return map;
    }
}
