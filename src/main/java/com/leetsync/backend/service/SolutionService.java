package com.leetsync.backend.service;

import com.leetsync.backend.dto.SolutionRequest;
import com.leetsync.backend.entity.Solution;
import com.leetsync.backend.repository.SolutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SolutionService {

    private final SolutionRepository solutionRepository;

    public SolutionService(SolutionRepository solutionRepository) {
        this.solutionRepository = solutionRepository;
    }

    public Solution saveSolution(SolutionRequest request) {
        Solution solution = new Solution();

        solution.setProblemNumber(request.getProblemNumber());
        solution.setProblemTitle(request.getProblemTitle());
        solution.setDifficulty(request.getDifficulty());
        solution.setLanguage(request.getLanguage());
        solution.setCode(request.getCode());
        solution.setSubmittedAt(request.getSubmittedAt());

        return solutionRepository.save(solution);
    }

    public List<Solution> getAllSolutions() {
        return solutionRepository.findAll();
    }

    public Optional<Solution> getSolutionById(Long id) {
        return solutionRepository.findById(id);
    }

    public boolean deleteSolution(Long id) {
        if (!solutionRepository.existsById(id)) {
            return false;
        }

        solutionRepository.deleteById(id);
        return true;
    }

    public Optional<Solution> updateSolution(Long id, SolutionRequest request) {
        return solutionRepository.findById(id)
                .map(solution -> {
                    solution.setProblemNumber(request.getProblemNumber());
                    solution.setProblemTitle(request.getProblemTitle());
                    solution.setDifficulty(request.getDifficulty());
                    solution.setLanguage(request.getLanguage());
                    solution.setCode(request.getCode());
                    solution.setSubmittedAt(request.getSubmittedAt());

                    return solutionRepository.save(solution);
                });
    }
}