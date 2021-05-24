/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.signers.batchV2;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.signers.TriphaseData;
import es.gob.afirma.signers.batchV2.JSONSingleSign.ProcessResult.Result;
import es.gob.afirma.signers.batchV2.JSONSingleSignConstants.SignFormat;
import es.gob.afirma.signers.batchV2.JSONSingleSignConstants.SignSubOperation;
import es.gob.afirma.triphase.server.document.DocumentManager;

/** Firma electr&oacute;nica &uacute;nica dentro de un lote. */
public final class JSONSingleSign {

	private static final String PROP_ID = "SignatureId"; //$NON-NLS-1$

	private static final String JSON_ATTRIBUTE_ID = "Id"; //$NON-NLS-1$

	private static final String JSON_ELEMENT_DATAREFERENCE = "datareference"; //$NON-NLS-1$
	private static final String JSON_ELEMENT_FORMAT = "format"; //$NON-NLS-1$
	private static final String JSON_ELEMENT_SUBOPERATION = "suboperation"; //$NON-NLS-1$
	private static final String JSON_ELEMENT_EXTRAPARAMS = "extraparams"; //$NON-NLS-1$

	static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private Properties extraParams;

	private String reference;

	private SignFormat format;

	private final String id;

	private SignSubOperation subOperation;

	private DocumentManager documentManager;

	private ProcessResult processResult = new ProcessResult(Result.NOT_STARTED, null);

	/** Crea una definici&oacute;n de tarea de firma electr&oacute;nica &uacute;nica.
	 * @param id Identificador de la firma. */
	JSONSingleSign(final String id) {
		this.id = id != null ? id : UUID.randomUUID().toString();
		this.extraParams = new Properties();
		// El identificador de la firma debe transmitirse al firmador trifasico a traves
		// de los extraParams para que este lo utilice y asi podamos luego asociar la
		// firma con los datos a los que corresponden
		this.extraParams.put(PROP_ID, getId());
	}

	/** Crea una definici&oacute;n de tarea de firma electr&oacute;nica &uacute;nica.
	 * @param id Identificador de la firma.
	 * @param dataSrc Datos a firmar.
	 * @param fmt Formato de firma.
	 * @param subOp Tipo de firma a realizar.
	 * @param xParams Opciones adicionales de la firma.
	 * @param ss Objeto para guardar la firma una vez completada. */
	public JSONSingleSign(final String id,
			          final String dataSrc,
			          final SignFormat fmt,
			          final SignSubOperation subOp,
			          final Properties xParams,
			          final DocumentManager ss) {

		if (dataSrc == null) {
			throw new IllegalArgumentException(
				"El origen de los datos a firmar no puede ser nulo" //$NON-NLS-1$
			);
		}

		if (fmt == null) {
			throw new IllegalArgumentException(
				"El formato de firma no puede ser nulo" //$NON-NLS-1$
			);
		}

		if (ss == null) {
			throw new IllegalArgumentException(
				"El objeto de guardado de firma no puede ser nulo" //$NON-NLS-1$
			);
		}

		this.reference = dataSrc;
		this.format = fmt;

		this.id = id != null ? id : UUID.randomUUID().toString();

		// El identificador de la firma debe transmitirse al firmador trifasico a traves
		// de los extraParams para que este lo utilice y asi podamos luego asociar la
		// firma con los datos a los que corresponden
		this.extraParams = xParams != null ? xParams : new Properties();
		this.extraParams.put(PROP_ID, getId());

		this.subOperation = subOp;
		this.documentManager = ss;
	}

	void save(final String signId, final byte[] dataToSave) throws IOException {
		this.documentManager.storeDocument(signId, null, dataToSave, null);
	}

	/**
	 * Recupera los par&aacute;metros de configuraci&oacute;n del formato de firma.
	 * @return Configuraci&oacute;n del formato de firma.
	 */
	public Properties getExtraParams() {
		return this.extraParams;
	}

	/**
	 * Recupera el formato de firma.
	 * @return Formato de firma.
	 */
	public SignFormat getSignFormat() {
		return this.format;
	}

	SignSubOperation getSubOperation() {
		return this.subOperation;
	}

	void setExtraParams(final Properties extraParams) {
		// El identificador de la firma debe transmitirse al firmador trifasico a traves
		// de los extraParams para que este lo utilice y asi podamos luego asociar la
		// firma con los datos a los que corresponden
		this.extraParams = extraParams != null ? extraParams : new Properties();
		this.extraParams.put(PROP_ID, getId());
	}

	public String getReference() {
		return this.reference;
	}

	void setReference(final String reference) {
		this.reference = reference;
	}

	void setFormat(final SignFormat format) {
		this.format = format;
	}

	void setSubOperation(final SignSubOperation subOperation) {
		this.subOperation = subOperation;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("{\n"); //$NON-NLS-1$
		sb.append(":\""); //$NON-NLS-1$
		sb.append(JSON_ATTRIBUTE_ID);
		sb.append(":\""); //$NON-NLS-1$
		sb.append(getId());
		sb.append("\" , \n\""); //$NON-NLS-1$
		sb.append(JSON_ELEMENT_DATAREFERENCE);
		sb.append("\":\""); //$NON-NLS-1$
		sb.append(this.reference);
		sb.append("\",\n"); //$NON-NLS-1$
		sb.append("\""); //$NON-NLS-1$
		sb.append(JSON_ELEMENT_FORMAT);
		sb.append("\":\""); //$NON-NLS-1$
		sb.append(getSignFormat().toString());
		sb.append("\",\n"); //$NON-NLS-1$
		sb.append("\""); //$NON-NLS-1$
		sb.append(JSON_ELEMENT_SUBOPERATION);
		sb.append("\":\""); //$NON-NLS-1$
		sb.append(getSubOperation().toString());
		sb.append("\",\n"); //$NON-NLS-1$
		sb.append("\""); //$NON-NLS-1$
		sb.append(JSON_ELEMENT_EXTRAPARAMS);
		sb.append("\":\""); //$NON-NLS-1$
		try {
			sb.append(AOUtil.properties2Base64(getExtraParams()));
		}
		catch (final IOException e) {
			LOGGER.severe(
				"Error convirtiendo los parametros adicionales de la firma '" + getId() + "' a Base64: " + e //$NON-NLS-1$ //$NON-NLS-2$
			);
		}
		sb.append("\",\n"); //$NON-NLS-1$
		sb.append("\""); //$NON-NLS-1$
		sb.append("\n}\n"); //$NON-NLS-1$

		return sb.toString();
	}

	/** Realiza el proceso de prefirma, incluyendo la descarga u obtenci&oacute;n de datos.
	 * @param certChain Cadena de certificados del firmante.
	 * @param algorithm Algoritmo de firma.
	 * @return Nodo <code>firma</code> del XML de datos trif&aacute;sicos (sin ninguna etiqueta
	 *         antes ni despu&eacute;s).
	 * @throws AOException Si hay problemas en la propia firma electr&oacute;nica.
	 * @throws IOException Si hay problemas en la obtenci&oacute;n, tratamiento o gradado de datos. */
	String doPreProcess(final X509Certificate[] certChain,
			            final JSONSingleSignConstants.SignAlgorithm algorithm,
			            final DocumentManager docManager) throws IOException,
			                                                                      AOException {
		return JSONSingleSignPreProcessor.doPreProcess(this, certChain, algorithm, docManager);
	}

	/** Obtiene la tarea de preproceso de firma para ser ejecutada en paralelo.
	 * @param certChain Cadena de certificados del firmante.
	 * @param algorithm Algoritmo de firma.
	 * @return Tarea de preproceso de firma para ser ejecutada en paralelo. */
	Callable<String> getPreProcessCallable(final X509Certificate[] certChain,
                                                  final JSONSingleSignConstants.SignAlgorithm algorithm,
                                                  final DocumentManager docManager) {
		return new PreProcessCallable(this, certChain, algorithm, docManager);
	}

	/** Realiza el proceso de postfirma, incluyendo la subida o guardado de datos.
	 * @param certChain Cadena de certificados del firmante.
	 * @param td Datos trif&aacute;sicos relativos <b>&uacute;nicamente</b> a esta firma.
	 *           Debe serializarse como un XML con esta forma (ejemplo):
	 *<pre>
	 * {
	 * "format":"PAdES",
	 * "signs":
	 * [{
	 *	"signinfo":[{
 	 *		"Id":"7725374e-728d-4a33-9db9-3a4efea4cead",
	 *		"params":
	 *			[{
	 *			"PRE":"PGGYzMC1iOTub3JnL1RSLzIwMDEvUkVDLXh",
	 *			"ENCODING":"UTF-8",
	 *			"NEED_PRE":"true",
	 *			"BASE":"PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGl"}]}]},
	 *{
	 *	"signinfo":[{
 	 *		"Id":"93d1531c-cd32-4c8e-8cc8-1f1cfe66f64a",
	 *		"params":
	 *			[{
	 *			"PRE":"MYIBAzAYBgkqhkiG9w0BCQMxCwYJKoZIhv",
	 *			"NEED_PRE":"true",
	 *			"TIME":"1621423575727",
	 *			"PID":"Wzw5MjBjODdmYmE4ZTEyZTM0YjU2OWUzOW"}]}]}]
	 * }
	 * </pre>
	 * @param algorithm Algoritmo de firma.
	 * @param batchId Identificador del lote de firma.
	 * @throws AOException Si hay problemas en la propia firma electr&oacute;nica.
	 * @throws IOException Si hay problemas en la obtenci&oacute;n, tratamiento o gradado de datos.
	 * @throws NoSuchAlgorithmException Si no se soporta alg&uacute;n algoritmo necesario. */
	void doPostProcess(final X509Certificate[] certChain,
			                  final TriphaseData td,
			                  final JSONSingleSignConstants.SignAlgorithm algorithm,
			                  final String batchId,
			                  final DocumentManager docManager) throws IOException,
			                                               AOException,
			                                               NoSuchAlgorithmException {
		JSONSingleSignPostProcessor.doPostProcess(
			this, certChain, td, algorithm, batchId, docManager
		);
	}

	/** Obtiene la tarea de postproceso de firma para ser ejecutada en paralelo.
	 * @param certChain Cadena de certificados del firmante.
	 * @param td Datos trif&aacute;sicos relativos <b>&uacute;nicamente</b> a esta firma.
	 *           Debe serializarse como un JSON con esta forma (ejemplo):
	 *           <pre>
	 *	{
 	 *	"signs":[
	 *		{	"id":"CADES-001",
 	 *			"result":"DONE_AND_SAVED",
 	 *			"description":""
	 *		},
	 *		{	"id":"XADES-002",
 	 *			"result":"DONE_AND_SAVED",
 	 *			"description":""
	 *		},
	 *		{	"id":"PADES-003",
 	 *			"result":"DONE_AND_SAVED",
 	 *			"description":""
	 *		}
	 *	]
	 * }
	 *           </pre>
	 * @param algorithm Algoritmo de firma.
	 * @param batchId Identificador del lote de firma.
	 * @return Tarea de postproceso de firma para ser ejecutada en paralelo. */
	Callable<CallableResult> getPostProcessCallable(final X509Certificate[] certChain,
			                                                          final TriphaseData td,
			                                                          final JSONSingleSignConstants.SignAlgorithm algorithm,
			                                                          final String batchId,
			                                                          final DocumentManager docManager) {
		return new PostProcessCallable(this, certChain, td, algorithm, batchId, docManager);
	}

	Callable<CallableResult> getSaveCallable(final JSONTempStore ts, final String batchId) {

		return new SaveCallable(this, this.documentManager, ts, batchId);
	}

	/**
	 * Recupera el identificador asignado en el lote a la firma.
	 * @return Identificador.
	 */
	public String getId() {
		return this.id;
	}

	void setProcessResult(final ProcessResult pResult) {
		this.processResult = pResult;
	}

	ProcessResult getProcessResult() {
		this.processResult.setId(getId());
		return this.processResult;
	}

	public String getDataSource() {
		return this.reference;
	}

	public void setDocumentManager(final DocumentManager documentManager) {
		this.documentManager = documentManager;
	}

	static class CallableResult {

		private final String signId;
		private final Exception exception;

		CallableResult(final String id) {
			this.signId = id;
			this.exception = null;
		}

		CallableResult(final String id, final Exception e) {
			this.signId = id;
			this.exception = e;
		}

		boolean isOk() {
			return this.exception == null;
		}

		Exception getError() {
			return this.exception;
		}

		String getSignatureId() {
			return this.signId;
		}
	}

	static final class ProcessResult {

		enum Result {
			NOT_STARTED,
			DONE_AND_SAVED,
			DONE_BUT_NOT_SAVED_YET,
			DONE_BUT_SAVED_SKIPPED,
			DONE_BUT_ERROR_SAVING,
			ERROR_PRE,
			ERROR_POST,
			SKIPPED,
			SAVE_ROLLBACKED;
		}

		private final Result result;
		private final String description;
		private String signId;

		boolean wasSaved() {
			return Result.DONE_AND_SAVED.equals(this.result);
		}

		static final ProcessResult PROCESS_RESULT_OK_UNSAVED = new ProcessResult(Result.DONE_BUT_NOT_SAVED_YET, null);
		static final ProcessResult PROCESS_RESULT_SKIPPED    = new ProcessResult(Result.SKIPPED,                null);
		static final ProcessResult PROCESS_RESULT_DONE_SAVED = new ProcessResult(Result.DONE_AND_SAVED,         null);
		static final ProcessResult PROCESS_RESULT_ROLLBACKED = new ProcessResult(Result.SAVE_ROLLBACKED,        null);

		ProcessResult(final Result r, final String d) {
			if (r == null) {
				throw new IllegalArgumentException(
					"El resultado no puede ser nulo" //$NON-NLS-1$
				);
			}
			this.result = r;
			this.description = d != null ? d : ""; //$NON-NLS-1$
		}

		@Override
		public String toString() {
			return "{\"id\":\"" + this.signId + "\",\n \"result\":\"" + this.result + "\",\n \"description\":\"" + this.description + "\"\n}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		void setId(final String id) {
			this.signId = id;
		}

		public Result getResult() {
			return this.result;
		}
	}


	static class PreProcessCallable implements Callable<String> {
		private final JSONSingleSign ss;
		private final X509Certificate[] certChain;
		private final JSONSingleSignConstants.SignAlgorithm algorithm;
		private final DocumentManager documentManager;

		public PreProcessCallable(final JSONSingleSign ss, final X509Certificate[] certChain,
                final JSONSingleSignConstants.SignAlgorithm algorithm,
                final DocumentManager docManager) {
			this.ss = ss;
			this.certChain = certChain;
			this.algorithm = algorithm;
			this.documentManager = docManager;
		}

		@Override
		public String call() throws Exception {
			return JSONSingleSignPreProcessor.doPreProcess(this.ss, this.certChain,
														this.algorithm, this.documentManager);
		}
	}

	static class PostProcessCallable implements Callable<CallableResult> {

		private final JSONSingleSign ss;
		private final X509Certificate[] certChain;
		private final TriphaseData td;
		private final JSONSingleSignConstants.SignAlgorithm algorithm;
		private final String batchId;
		private final DocumentManager documentManager;

		public PostProcessCallable(final JSONSingleSign ss, final X509Certificate[] certChain,
                final TriphaseData td, final JSONSingleSignConstants.SignAlgorithm algorithm,
                final String batchId, final DocumentManager docManager) {
			this.ss = ss;
			this.certChain = certChain;
			this.td = td;
			this.algorithm = algorithm;
			this.batchId = batchId;
			this.documentManager = docManager;
		}

		@Override
		public CallableResult call() {
			try {
				JSONSingleSignPostProcessor.doPostProcess(this.ss, this.certChain, this.td,
														this.algorithm, this.batchId, this.documentManager);
			}
			catch(final Exception e) {
				return new CallableResult(this.ss.getId(), e);
			}
			return new CallableResult(this.ss.getId());
		}
	}

	static class SaveCallable implements Callable<CallableResult> {

		private final JSONSingleSign ss;
		private final DocumentManager documentManager;
		private final JSONTempStore ts;
		private final String batchId;

		public SaveCallable(final JSONSingleSign ss, final DocumentManager documentManager, final JSONTempStore ts, final String batchId) {
			this.ss = ss;
			this.documentManager = documentManager;
			this.ts = ts;
			this.batchId = batchId;
		}

		@Override
		public CallableResult call() {
			try {
				final byte[] dataToSave = this.ts.retrieve(this.ss, this.batchId);
				this.documentManager.storeDocument(this.ss.getId(), null, dataToSave, null);
			}
			catch(final Exception e) {
				LOGGER.warning("No se puede recuperar para su guardado como firma el recurso: " + this.ss.getId()); //$NON-NLS-1$
				return new CallableResult(this.ss.getId(), e);
			}
			return new CallableResult(this.ss.getId());
		}
	}
}
