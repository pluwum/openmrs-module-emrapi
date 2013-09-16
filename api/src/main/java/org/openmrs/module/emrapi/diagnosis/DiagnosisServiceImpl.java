package org.openmrs.module.emrapi.diagnosis;

import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.ObsService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.emrapi.EmrApiProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiagnosisServiceImpl extends BaseOpenmrsService implements DiagnosisService {

	private EmrApiProperties emrApiProperties;

	private ObsService obsService;

	public void setEmrApiProperties(EmrApiProperties emrApiProperties) {
		this.emrApiProperties = emrApiProperties;
	}

	public void setObsService(ObsService obsService) {
		this.obsService = obsService;
	}

	@Override
	public Obs codeNonCodedDiagnosis(Obs nonCodedObs, Concept codedDiagnosis) {

		if ((nonCodedObs != null) && (codedDiagnosis != null)) {
			Concept codedDiagnosisConcept = emrApiProperties.getDiagnosisMetadata().getCodedDiagnosisConcept();
			nonCodedObs.setConcept(codedDiagnosisConcept);
			nonCodedObs.setValueCoded(codedDiagnosis);
			nonCodedObs.setValueText("");
			nonCodedObs = obsService.saveObs(nonCodedObs, "code a diagnosis");
			return nonCodedObs;

		}
		return null;
	}

	@Override
	public List<Diagnosis> getDiagnoses(Patient patient, Date fromDate) {
		List<Diagnosis> diagnoses = new ArrayList<Diagnosis>();

		DiagnosisMetadata diagnosisMetadata = emrApiProperties.getDiagnosisMetadata();

		List<Obs> observations = obsService.getObservations(Arrays.asList((Person) patient), null, Arrays.asList(diagnosisMetadata.getDiagnosisSetConcept()),
				null, null, null, Arrays.asList("obsDatetime"),
				null, null, fromDate, null, false);

		for (Obs obs : observations) {
			Diagnosis diagnosis = diagnosisMetadata.toDiagnosis(obs);

			Collection<Concept> nonDiagnosisConcepts = emrApiProperties.getSuppressedDiagnosisConcepts();
			Collection<Concept> nonDiagnosisConceptSets = emrApiProperties.getNonDiagnosisConceptSets();

			Set<Concept> filter = new HashSet<Concept>();
			filter.addAll(nonDiagnosisConcepts);
			for (Concept conceptSet : nonDiagnosisConceptSets) {
				filter.addAll(conceptSet.getSetMembers());
			}

			if (!filter.contains(diagnosis.getDiagnosis().getCodedAnswer())) {
				diagnoses.add(diagnosis);
			}
		}

		return diagnoses;
	}
}