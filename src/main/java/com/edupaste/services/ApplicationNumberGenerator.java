package com.edupaste.services;

import com.edupaste.models.SchoolApplicationSequence;
import com.edupaste.repositories.SchoolApplicationSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ApplicationNumberGenerator {

    @Autowired
    private SchoolApplicationSequenceRepository sequenceRepository;

    @Transactional
    public String generateNextApplicationNumber(Long schoolId) {
        int currentYear = LocalDate.now().getYear();
        String schoolPrefix = "SCH" + schoolId;

        Optional<SchoolApplicationSequence> seqOpt = sequenceRepository.findBySchoolIdAndCurrentYearForUpdate(schoolId, currentYear);

        SchoolApplicationSequence sequence;
        if (seqOpt.isPresent()) {
            sequence = seqOpt.get();
            sequence.setLastSequence(sequence.getLastSequence() + 1);
        } else {
            sequence = new SchoolApplicationSequence();
            sequence.setSchoolId(schoolId);
            sequence.setCurrentYear(currentYear);
            sequence.setLastSequence(1);
        }

        sequence = sequenceRepository.save(sequence);

        return String.format("%s-ADM-%d-%06d", schoolPrefix, currentYear, sequence.getLastSequence());
    }
}
