package com.baytalhikmah.sovereign.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** With the profile leak-guard, the connection watchdog in ConnectionLeases runs on its own timer. */
@Configuration(proxyBeanMethods = false)
@Profile("leak-guard")
@EnableScheduling
public class LeakGuardConfig {
}
