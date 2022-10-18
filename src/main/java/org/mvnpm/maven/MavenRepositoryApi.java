package org.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mvnpm.Constants;
import org.mvnpm.file.FileClient;
import org.mvnpm.file.FileStore;
import org.mvnpm.file.FileType;
import org.mvnpm.npm.NpmRegistryClient;
import org.mvnpm.npm.model.FullName;
import org.mvnpm.npm.model.Package;
import org.mvnpm.npm.model.Project;

/**
 * The maven repository endpoint
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add source jar
 * TODO: Add metadata xml
 */
@Path("/maven2")
public class MavenRepositoryApi {

    @RestClient 
    NpmRegistryClient npmRegistryClient;
    
    @Inject
    FileClient fileClient; 
    
    @Inject 
    FileStore fileStore;
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getAny(@PathParam("gavt") String gavt){
        
        NameVersionType nameVersionType = UrlPathParser.parse(gavt);
        
        if(nameVersionType.sha1()){
            return getSha1(nameVersionType.name(), nameVersionType.version(), nameVersionType.type());
        } else {
            return getFile(nameVersionType.name(), nameVersionType.version(), nameVersionType.type());                
        }
    }
    
    private Uni<Response> getFile(FullName fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getFile(fullName, latest, type);
            });
        }else {
            Uni<Package> npmPackage = npmRegistryClient.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                String filename = fileStore.getLocalFileName(type, p);
                return fileClient.streamFile(type, p).onItem().transform((file) -> {
                        return Response.ok(file).header(Constants.HEADER_CONTENT_DISPOSITION_KEY, Constants.HEADER_CONTENT_DISPOSITION_VALUE + Constants.DOUBLE_QUOTE + filename + Constants.DOUBLE_QUOTE)
                            .build();
                });
            }); 
        }
    }
    
    private Uni<Response> getSha1(FullName fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getSha1(fullName, latest, type);
            });
        }else {
            Uni<Package> npmPackage = npmRegistryClient.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                String filename = fileStore.getLocalSha1FileName(type, p);
                return fileClient.streamSha1(type, p).onItem().transform((file) -> {
                        return Response.ok(file).header(Constants.HEADER_CONTENT_DISPOSITION_KEY, Constants.HEADER_CONTENT_DISPOSITION_VALUE + Constants.DOUBLE_QUOTE + filename + Constants.DOUBLE_QUOTE)
                            .build();
                });
            }); 
        }
    }
    
    private Uni<String> getLatestVersion(FullName fullName){
        Uni<Project> project = npmRegistryClient.getProject(fullName.npmFullName());
        return project.onItem()
                .transform((p) -> {
                    return p.distTags().latest();
                });
    }
}