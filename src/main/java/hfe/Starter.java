package hfe;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.logging.Logger;

@SpringBootApplication
public class Starter {

    private static final String NAME = "HFE_DEP";

    public Starter() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Starter.class);
        ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl)context.getBean(ProcessEngineConfiguration.class);
        config.setTelemetryReporter(null);

        RepositoryService repositoryService = context.getBean(RepositoryService.class);
        addBpm(repositoryService, createModelInstance());
    }

    static void addBpm(RepositoryService repositoryService, BpmnModelInstance modelInstance) {

        Bpmn.validateModel(modelInstance);

        Bpmn.writeModelToFile(new File("hfe.bpmn"), modelInstance);

        Deployment depl = repositoryService.createDeploymentQuery().deploymentName(NAME).singleResult();
        /*
        for(Deployment instance : processEngine.getRepositoryService().createDeploymentQuery().list()) {
            processEngine.getRepositoryService().deleteDeployment(instance.getId(), true);
        }*/
        if(depl == null) {
            Deployment deployment = repositoryService.createDeployment().name(NAME).addModelInstance(NAME + ".bpmn", modelInstance).deploy();
            Logger.getLogger("HFE").info("Camunda: deployt: " + deployment.getId());
        }
    }


    static BpmnModelInstance createModelInstance() {
        return Bpmn.createExecutableProcess()
                .name("bAre Massenupdate")
                .startEvent().name("Start")
                .userTask().name(BulkUpdateBareDefinition.FIN_USER_TASK)
                .id("fin")

                .exclusiveGateway("click_fin_saved_new").name("")
                .condition("", String.format("${action == '%s'}", BulkUpdateWorkflowStatus.FIN_COMPLETED))
                .serviceTask().name("Vervollstaendigen")
                .camundaClass(FinComplete.class)
                .userTask().name(BulkUpdateBareDefinition.FIN_FINC_USER_TASK)

                .exclusiveGateway("click_fin_completed").name("")
                .condition("", String.format("${action == '%s'}", BulkUpdateWorkflowStatus.FINC_COMPLETED))
                .serviceTask().name("Ausfuehren(Finc)")
                .camundaClass(FincComplete.class)
                .endEvent().name("ausgefuehrt")

                .moveToNode("click_fin_completed")
                .condition("", String.format("${action == '%s'}", BulkUpdateWorkflowStatus.FINC_REJECTED))
                .serviceTask().name("Zurueckweisen(Finc)")
                .camundaClass(FincReject.class)
                .connectTo("fin")

                .moveToNode("click_fin_completed")
                .condition("", String.format("${action == '%s'}", BulkUpdateWorkflowStatus.FIN_REVIVED))
                .serviceTask().name("Zurueckholen(Fin)")
                .camundaClass(FinRevive.class)
                .connectTo("fin")

                .moveToNode("click_fin_saved_new")
                .condition("", String.format("${action == '%s'}", BulkUpdateWorkflowStatus.FIN_SAVED))
                .serviceTask().name("Speichern")
                .camundaClass(FinSave.class)
                .connectTo("fin")

                .moveToNode("click_fin_saved_new")
                .condition("", String.format("${action == '%s'}", BulkUpdateWorkflowStatus.FIN_CANCELED))
                .serviceTask().name("verwerfen")
                .camundaClass(FinCancel.class)
                .endEvent().name("verworfen")

                .done();
    }

    public enum BulkUpdateWorkflowStatus {

        NEW(null),
        FIN_SAVED(BulkUpdateBareDefinition.FIN_USER_TASK),
        FIN_CANCELED(BulkUpdateBareDefinition.FIN_USER_TASK),
        FIN_COMPLETED(BulkUpdateBareDefinition.FIN_USER_TASK),
        FIN_REVIVED(BulkUpdateBareDefinition.FIN_FINC_USER_TASK),
        FINC_REJECTED(BulkUpdateBareDefinition.FIN_FINC_USER_TASK),
        FINC_COMPLETED(BulkUpdateBareDefinition.FIN_FINC_USER_TASK);

        private String taskName;
        BulkUpdateWorkflowStatus(String taskName) {
            this.taskName = taskName;
        }
    }

    public static class BulkUpdateBareDefinition  {
        public static final String FIN_USER_TASK = "FIN_AKTION";
        public static final String FIN_FINC_USER_TASK = "Fin_Finc_AKTION";
    }
}
