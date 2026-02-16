package kr.co.devsign.devsign_backend.Service;

import kr.co.devsign.devsign_backend.Entity.AssemblyProject;
import kr.co.devsign.devsign_backend.Entity.AssemblyReport;
import kr.co.devsign.devsign_backend.Repository.AssemblyProjectRepository;
import kr.co.devsign.devsign_backend.Repository.AssemblyReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AssemblyService {

    private final AssemblyReportRepository reportRepository;
    private final AssemblyProjectRepository projectRepository;

    public Map<String, Object> getMySubmissions(String loginId, int year, int semester) {
        List<AssemblyReport> reports =
                reportRepository.findByLoginIdAndYearAndSemesterOrderByMonthAsc(loginId, year, semester);

        if (reports.isEmpty()) {
            int[] months = (semester == 1) ? new int[]{3, 4, 5, 6} : new int[]{9, 10, 11, 12};
            for (int month : months) {
                AssemblyReport r = new AssemblyReport();
                r.setLoginId(loginId);
                r.setYear(year);
                r.setSemester(semester);
                r.setMonth(month);
                r.setStatus("미제출");
                r.setType(month == 3 || month == 9 ? "계획서" : (month == 6 || month == 12 ? "결과물" : "진행보고"));
                reportRepository.save(r);
            }
            reports = reportRepository.findByLoginIdAndYearAndSemesterOrderByMonthAsc(loginId, year, semester);
        }

        String projectTitle = projectRepository.findByLoginIdAndYearAndSemester(loginId, year, semester)
                .map(AssemblyProject::getTitle)
                .orElse("");

        Map<String, Object> response = new HashMap<>();
        response.put("reports", reports);
        response.put("projectTitle", projectTitle);
        return response;
    }

    public void saveProjectTitle(Map<String, Object> params) {
        String loginId = (String) params.get("loginId");
        int year = Integer.parseInt(params.get("year").toString());
        int semester = Integer.parseInt(params.get("semester").toString());
        String title = (String) params.get("title");

        AssemblyProject project = projectRepository.findByLoginIdAndYearAndSemester(loginId, year, semester)
                .orElse(new AssemblyProject());

        project.setLoginId(loginId);
        project.setYear(year);
        project.setSemester(semester);
        project.setTitle(title);

        projectRepository.save(project);
    }

    public String submitFiles(
            String loginId,
            String reportId,
            int year,
            int semester,
            int month,
            String memo,
            MultipartFile presentation,
            MultipartFile pdf,
            MultipartFile other
    ) throws Exception {

        AssemblyReport report = null;

        if (reportId != null && !reportId.equals("0") && !reportId.startsWith("temp")) {
            report = reportRepository.findById(Long.parseLong(reportId)).orElse(null);
        }

        if (report == null) {
            List<AssemblyReport> existing =
                    reportRepository.findByLoginIdAndYearAndSemesterOrderByMonthAsc(loginId, year, semester);

            report = existing.stream()
                    .filter(r -> r.getMonth() == month)
                    .findFirst()
                    .orElse(null);
        }

        if (report == null) {
            report = new AssemblyReport();
            report.setLoginId(loginId);
            report.setYear(year);
            report.setSemester(semester);
            report.setMonth(month);
            report.setStatus("미제출");
            report.setType(month == 3 || month == 9 ? "계획서" : (month == 6 || month == 12 ? "결과물" : "진행보고"));
        }

        String uploadBase = System.getProperty("user.dir") + File.separator + "uploads";
        String userPath = uploadBase + File.separator + loginId + File.separator + month;

        File folder = new File(userPath);
        if (!folder.exists()) folder.mkdirs();

        if (presentation != null && !presentation.isEmpty()) {
            String fileName = "pres_" + presentation.getOriginalFilename();
            presentation.transferTo(new File(userPath + File.separator + fileName));
            report.setPresentationPath(userPath + File.separator + fileName);
        }

        if (pdf != null && !pdf.isEmpty()) {
            String fileName = "pdf_" + pdf.getOriginalFilename();
            pdf.transferTo(new File(userPath + File.separator + fileName));
            report.setPdfPath(userPath + File.separator + fileName);
        }

        if (other != null && !other.isEmpty()) {
            String fileName = "other_" + other.getOriginalFilename();
            other.transferTo(new File(userPath + File.separator + fileName));
            report.setOtherPath(userPath + File.separator + fileName);
        }

        report.setMemo(memo);
        report.setStatus("제출완료");
        report.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));

        reportRepository.save(report);
        return "제출 성공";
    }
}

