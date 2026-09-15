package com.ll.demo01;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComponentA {
    private final com.ll.demo01.ComponentB componentB;
    private final com.ll.demo01.ComponentC componentC;
    private final com.ll.demo01.ComponentD componentD;
    private final com.ll.demo01.ComponentE componentE;

    public String action() {
        return "ComponentA action / " + componentB.getAction();
    }
}