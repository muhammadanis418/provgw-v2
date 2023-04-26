package com.rockville.wariddn.provgwv2.config;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import org.kie.api.runtime.KieSession;

@Configuration
@Slf4j
public class DroolsConfig {
    
    @Value("${drools.file.name}")
    private String droolFileName;
    private KieServices kieServices = KieServices.Factory.get();
    
    @Bean
    public KieContainer getKieContainer() throws IOException {
        getKieRepository();
        KieBuilder kb = kieServices.newKieBuilder(getKieFileSystem());
        kb.buildAll();
        KieModule kieModule = kb.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
    
    private void getKieRepository() {
        final KieRepository kieRepository = kieServices.getRepository();
        kieRepository.addKieModule(new KieModule() {
            @Override
            public ReleaseId getReleaseId() {
                return kieRepository.getDefaultReleaseId();
            }
        });
    }
    
    private KieFileSystem getKieFileSystem() throws IOException {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        kieFileSystem.write(ResourceFactory.newFileResource(new File(droolFileName)));
//        kieFileSystem.write(ResourceFactory.newClassPathResource(droolFileName));
        return kieFileSystem;
    }
    
    @Bean
    public KieSession getKieSession() throws IOException {
        KieSession kieSession = getKieContainer().newKieSession();
//        kieSession.addEventListener(new RuleRuntimeEventListener() {
//            @Override
//            public void objectInserted(ObjectInsertedEvent event) {
//                log.info("Object inserted \n "
//                        + event.getObject().toString());
//            }
//
//            @Override
//            public void objectUpdated(ObjectUpdatedEvent event) {
//                log.info("Object was updated \n"
//                        + "New Content \n"
//                        + event.getObject().toString());
//            }
//
//            @Override
//            public void objectDeleted(ObjectDeletedEvent event) {
//                log.info("Object retracted \n"
//                        + event.getOldObject().toString());
//            }
//        });
        return kieSession;
    }
}
