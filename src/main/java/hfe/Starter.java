package hfe;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

@SpringBootApplication
public class Starter {

    private static final String NAME = "HFE_DEP";
    public static final String FIN_USER_TASK = "FIN_AKTION";
    public static final String FIN_FINC_USER_TASK = "Fin_Finc_AKTION";

    public Starter() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Starter.class);
        ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl)context.getBean(ProcessEngineConfiguration.class);
        config.setTelemetryReporter(null);

        RepositoryService repositoryService = context.getBean(RepositoryService.class);
        String id = addBpm(repositoryService, createModelInstance());

        getTasks(context.getBean(ProcessEngine.class), id);
    }

    static String addBpm(RepositoryService repositoryService, BpmnModelInstance modelInstance) {

        Bpmn.validateModel(modelInstance);

        Bpmn.writeModelToFile(new File("hfe.bpmn"), modelInstance);

        Deployment depl = repositoryService.createDeploymentQuery().deploymentName(NAME).singleResult();
        /*
        for(Deployment instance : processEngine.getRepositoryService().createDeploymentQuery().list()) {
            processEngine.getRepositoryService().deleteDeployment(instance.getId(), true);
        }*/

        String id = null;
        if(depl == null) {
            Deployment deployment = repositoryService.createDeployment().name(NAME).addModelInstance(NAME + ".bpmn", modelInstance).deploy();
            Logger.getLogger("HFE").info("Camunda: deployt: " + deployment.getId());
            id = deployment.getId();
        }

        return id;
    }

    public static void getTasks(ProcessEngine processEngine, String id) {
        processEngine.getManagementService().
        List<ProcessInstance> instances = processEngine.getRuntimeService().().rootProcessInstances().list();
        List<Task> tasks = processEngine.getTaskService().createTaskQuery().processInstanceId(instances.get(0).getId()).list();
        System.out.println();
    }


    static BpmnModelInstance createModelInstance() {
        return Bpmn.createExecutableProcess()
                .name("bAre Massenupdate")
                .startEvent().name("Start")
                .userTask().name(FIN_USER_TASK)
                .id("fin")

                .exclusiveGateway("click_fin_saved_new").name("")
                .condition("", String.format("${action == '%s'}", Status.FIN_COMPLETED))
                .serviceTask().name("Vervollstaendigen")
                .camundaClass(FinComplete.class)
                .userTask().name(FIN_FINC_USER_TASK)

                .exclusiveGateway("click_fin_completed").name("")
                .condition("", String.format("${action == '%s'}", Status.FINC_COMPLETED))
                .serviceTask().name("Ausfuehren(Finc)")
                .camundaClass(FincComplete.class)
                .endEvent().name("ausgefuehrt")

                .moveToNode("click_fin_completed")
                .condition("", String.format("${action == '%s'}", Status.FINC_REJECTED))
                .serviceTask().name("Zurueckweisen(Finc)")
                .camundaClass(FincReject.class)
                .connectTo("fin")

                .moveToNode("click_fin_completed")
                .condition("", String.format("${action == '%s'}", Status.FIN_REVIVED))
                .serviceTask().name("Zurueckholen(Fin)")
                .camundaClass(FinRevive.class)
                .connectTo("fin")

                .moveToNode("click_fin_saved_new")
                .condition("", String.format("${action == '%s'}", Status.FIN_SAVED))
                .serviceTask().name("Speichern")
                .camundaClass(FinSave.class)
                .connectTo("fin")

                .moveToNode("click_fin_saved_new")
                .condition("", String.format("${action == '%s'}", Status.FIN_CANCELED))
                .serviceTask().name("verwerfen")
                .camundaClass(FinCancel.class)
                .endEvent().name("verworfen")

                .done();
    }

    public enum Status {

        NEW(null),
        FIN_SAVED(FIN_USER_TASK),
        FIN_CANCELED(FIN_USER_TASK),
        FIN_COMPLETED(FIN_USER_TASK),
        FIN_REVIVED(FIN_FINC_USER_TASK),
        FINC_REJECTED(FIN_FINC_USER_TASK),
        FINC_COMPLETED(FIN_FINC_USER_TASK);

        private String taskName;
        Status(String taskName) {
            this.taskName = taskName;
        }
    }

}
