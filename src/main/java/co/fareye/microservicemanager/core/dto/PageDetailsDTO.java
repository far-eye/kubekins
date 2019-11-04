package co.fareye.microservicemanager.core.dto;

public class PageDetailsDTO {
    private Integer pageNo=0;
    private Integer recordsPerPage=50;
    private String sortOn="id";
    private String sortType="ASC";

    public PageDetailsDTO(){}

    public PageDetailsDTO(Integer pageNo,Integer recordsPerPage,String sortOn,String sortType)
    {
        if(pageNo!=null) {
            this.pageNo=pageNo;
        }
        if(recordsPerPage!=null) {
            this.recordsPerPage=recordsPerPage;
        }
        if(sortOn!=null ) {
            this.sortOn=sortOn;
        }
        if(sortType!=null) {
            this.sortType=sortType;
        }
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getRecordsPerPage() {
        return recordsPerPage;
    }

    public void setRecordsPerPage(Integer recordsPerPage) {
        this.recordsPerPage = recordsPerPage;
    }

    public String getSortOn() {
        return sortOn;
    }

    public void setSortOn(String sortOn) {
        this.sortOn = sortOn;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }
}