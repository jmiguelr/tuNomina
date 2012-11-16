package com.sim.credito

import com.sim.catalogo.SimCatEtapaPrestamo
import com.sim.cliente.RsCliente
import com.sim.tablaAmortizacion.TablaAmortizacion
import com.sim.tablaAmortizacion.TablaAmortizacionServiceException
import org.grails.activiti.ApprovalStatus

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.springframework.dao.DataIntegrityViolationException

import com.sim.alfresco.AlfrescoService;

class PrestamoController {
	
	def tablaAmortizacionService

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
    static activiti = true

    def index = {
        redirect(action: "list", params: params)
    }

    def start = {
        start(params)
    }
	
    def list = {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [prestamoInstanceList: Prestamo.list(params), 
			   prestamoInstanceTotal: Prestamo.count(),
			   myTasksCount: assignedTasksCount]
    }

    def create = {
        def prestamoInstance = new Prestamo()
        prestamoInstance.properties = params
        return [prestamoInstance: prestamoInstance,
			          myTasksCount: assignedTasksCount]
    }

    def save = {
        def prestamoInstance = new Prestamo(params)
		if (params.complete) {
			prestamoInstance.estatusSolicitud = SimCatEtapaPrestamo.findByClaveEtapaPrestamo('CAPTURADA_MESA')
		}else{
			prestamoInstance.estatusSolicitud = SimCatEtapaPrestamo.findByClaveEtapaPrestamo('INICIO_MESA')
		}
		prestamoInstance.approvalStatus = ApprovalStatus.PENDING
		prestamoInstance.documentosCorrectos = false
		prestamoInstance.aprobado = false
		prestamoInstance.reenviarSolicitud = false
		if (!params.correoSolicitante){
			log.info("No se asigno correo al solicitante")
			prestamoInstance.correoSolicitante = "sincorreo@gmail.com"
		}
        if (prestamoInstance.save(flush: true)) {
            flash.message = "${message(code: 'default.created.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), prestamoInstance.id])}"
			      params.id = prestamoInstance.id
						if (params.complete) {
							//LOS SIGUIENTES PARAMETROS CAUSABAN PROBLEMAS CON ACTIVITI
							//SIN EMBARGO SI PASA CORRECTAMENTE LOS ID DE CADA PARAMETRO ELIMINADO
							params.remove("dependencia")
							params.remove("promocion")
							params.remove("sucursal")
							params.remove("delegacion")
							params.remove("vendedor")
							params.remove("estatusSolicitud")
							params.remove("formaDeDispercion")
							params.remove("cliente")
							completeTask(params)
						} else {
							params.action="show"
							saveTask(params)
						}
            redirect(action: "show", params: params)
        }
        else {
            render(view: "create", model: [prestamoInstance: prestamoInstance, myTasksCount: assignedTasksCount])
        }
    }

    def show = {
        def prestamoInstance = Prestamo.get(params.id)
        if (!prestamoInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), params.id])}"
            redirect(controller: "task", action: "myTaskList")
        }
        else {

			try{
				List<Document> documentos = new ArrayList<Document>();
				AlfrescoService service = new AlfrescoService();
				Object o = null
				
				//try{
					o=service.getByPath("/Sites/tuNomina/creditos/${prestamoInstance.cliente.id}/${prestamoInstance.clavePrestamo}");
				//}catch(Exception e){
					//e.printStackTrace();
				//}
				
				if(o!=null){
					Folder folder = (Folder)o;
					
					for(CmisObject cmisObject: folder.getChildren()){
						if(cmisObject instanceof Document){
							documentos.add((Document) cmisObject);
						}
					}
					
					request.putAt("documentos", documentos);
				}
			}catch(Exception e){
					e.printStackTrace();
			}
            [prestamoInstance: prestamoInstance, myTasksCount: assignedTasksCount]
        }
    }

    def edit = {
        def prestamoInstance = Prestamo.get(params.id)
        if (!prestamoInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), params.id])}"
            redirect(controller: "task", action: "myTaskList")
        }
        else {
            [prestamoInstance: prestamoInstance, myTasksCount: assignedTasksCount]
        }
    }

    def update = {
        def prestamoInstance = Prestamo.get(params.id)
        if (prestamoInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (prestamoInstance.version > version) {
                    
                    prestamoInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'prestamo.label', default: 'Prestamo')] as Object[], "Another user has updated this Prestamo while you were editing")
                    render(view: "edit", model: [prestamoInstance: prestamoInstance, myTasksCount: assignedTasksCount])
                    return
                }
            }
            prestamoInstance.properties = params
			
			def estatusSolicitud = SimCatEtapaPrestamo.get(params.estatusSolicitud.id)
			
			log.info("Estatus de la solicitud: "+estatusSolicitud)
			
			Boolean isComplete = params["_action_update"].equals(message(code: 'default.button.complete.label', default: 'Complete'))
			if (isComplete) {
				
				if (estatusSolicitud.claveEtapaPrestamo.equals("INICIO_MESA")){
					prestamoInstance.estatusSolicitud = SimCatEtapaPrestamo.findByClaveEtapaPrestamo('CAPTURADA_MESA')
					prestamoInstance.approvalStatus = ApprovalStatus.PENDING
				}else if(estatusSolicitud.claveEtapaPrestamo.equals("CAPTURADA_MESA") && params.aprobado.equals("on")){
					prestamoInstance.estatusSolicitud = SimCatEtapaPrestamo.findByClaveEtapaPrestamo('PROCESADA')
					params.from = grailsApplication.config.activiti.mailServerDefaultFrom
					params.emailTo = prestamoInstance.correoSolicitante
					log.info("ID CLIENTE: "+params.cliente.id)
					def clientePrestamo = RsCliente.get(params.cliente.id)
					String nombreCliente = clientePrestamo.persona.primerNombre + " " + clientePrestamo.persona.apellidoPaterno
					log.info("NOMBRE CLIENTE: "+nombreCliente)
					params.nombreCliente = nombreCliente
					prestamoInstance.approvalStatus = ApprovalStatus.PENDING
				}else if(estatusSolicitud.claveEtapaPrestamo.equals("CAPTURADA_MESA") && !params.aprobado.equals("on")){
					prestamoInstance.estatusSolicitud = SimCatEtapaPrestamo.findByClaveEtapaPrestamo('DEVOLUCION_AMESA')
					prestamoInstance.approvalStatus = ApprovalStatus.REJECTED
				}else if(estatusSolicitud.claveEtapaPrestamo.equals("DEVOLUCION_AMESA") && params.reenviarSolicitud.equals("on")){
					prestamoInstance.estatusSolicitud = SimCatEtapaPrestamo.findByClaveEtapaPrestamo('CAPTURADA_MESA')
					prestamoInstance.approvalStatus = ApprovalStatus.PENDING
				}
				
			}
			
            if (!prestamoInstance.hasErrors() && prestamoInstance.save(flush: true)) {
                flash.message = "${message(code: 'default.updated.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), prestamoInstance.id])}"
								
								if (isComplete) {
										params.aprobado = params.aprobado.equals("on")
										params.reenviarSolicitud = prestamoInstance.reenviarSolicitud
										//RECUPERA EL ESTATUS DE LA SOLICITUD ACTUAL
										params.estatusSolicitudActual =  prestamoInstance.estatusSolicitud.claveEtapaPrestamo
										//LOS SIGUIENTES PARAMETROS CAUSABAN PROBLEMAS CON ACTIVITI
										//SIN EMBARGO SI PASA CORRECTAMENTE LOS ID DE CADA PARAMETRO ELIMINADO
										params.remove("dependencia")
										params.remove("promocion")
										params.remove("sucursal")
										params.remove("delegacion")
										params.remove("vendedor")
										params.remove("estatusSolicitud")
										params.remove("formaDeDispercion")
										params.remove("cliente")
										completeTask(params)
								} else {
										params.action="show"
										saveTask(params)
								}				
                redirect(action: "show", id: prestamoInstance.id, params: [taskId:params.taskId, complete:isComplete?:null])
            }
            else {
                render(view: "edit", model: [prestamoInstance: prestamoInstance, myTasksCount: assignedTasksCount])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), params.id])}"
            redirect(controller: "task", action: "myTaskList")
        }
    }

    def delete = {
        def prestamoInstance = Prestamo.get(params.id)
        if (prestamoInstance) {
            try {
                prestamoInstance.delete(flush: true)
                deleteTask(params.taskId)
                flash.message = "${message(code: 'default.deleted.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), params.id])}"
                redirect(controller: "task", action: "myTaskList")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = "${message(code: 'default.not.deleted.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), params.id])}"
                redirect(action: "show", id: params.id, params: params)
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'prestamo.label', default: 'Prestamo'), params.id])}"
            redirect(controller: "task", action: "myTaskList")
        }
    }
	
	def generaTablaAmortizacion = {
		log.info(params.idPrestamo)
		Prestamo prestamoInstance = Prestamo.get(params.idPrestamo)
	
		/*
		//OBTIENE TODOS LOS REGISTROS DE LA TABLA DE AMORTIZACION QUE PERTENECEN AL PRESTAMO
		def registrosTabla = TablaAmortizacion.findAllByPrestamo(prestamoInstance)
		
		registrosTabla.each() { 
			prestamoInstance.removeFromTablaAmortizacion(it)
		}
		prestamoInstance.save(flush: true)
		*/
		
		try{
			tablaAmortizacionService.generaTablaAmortizacion(prestamoInstance)
		//VERIFICAR SI SE GENERO ALGUN ERROR
		}catch(TablaAmortizacionServiceException errorGeneraTablaAmor){
			prestamoInstance.errors.reject("ErrorGeneraTablaAmor",errorGeneraTablaAmor.mensaje)
			log.error "Failed:", errorGeneraTablaAmor
			//AL APLICAR LA FUNCIONALIDAD REAL HAY QUE MOSTRAR EL MENSAJE
			redirect(action: "show", params: [id: params.idPrestamo])
			return
		}
		
		redirect(controller: "tablaAmortizacion", action: "list", params: [idPrestamo: params.idPrestamo])
		
	}
	
}
