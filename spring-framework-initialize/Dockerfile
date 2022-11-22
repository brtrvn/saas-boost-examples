FROM tomcat:9-jdk8-corretto AS build
RUN amazon-linux-extras install epel -y && yum install -y xmlstarlet
RUN --mount=type=bind,target=/data \
    TLD_EXCLUDES=$(jars=""; \
    for jar in $(ls /data/target/SpringFrameworkInitialize/WEB-INF/lib/*.jar | grep -v jstl-1.2.jar); \
        do jars+="$(basename $jar),"; \
    done; \
    echo $jars | sed 's/,$//'); \
    xmlstarlet ed -L \
    -s "/Context[not(JarScanner)]" -t elem -n "JarScanner" \
    -s "/Context/JarScanner[not(JarScanFilter)]" -t elem -n "JarScanFilter" \
    -s "/Context/JarScanner/JarScanFilter[last()]" -t attr -n "tldSkip" -v "\${tomcat.util.scan.StandardJarScanFilter.jarsToSkip},$TLD_EXCLUDES" \
    /usr/local/tomcat/conf/context.xml
#RUN echo "org.apache.jasper.servlet.TldScanner.level = FINE" >> /usr/local/tomcat/conf/logging.properties

FROM tomcat:9-jdk8-corretto
COPY --from=build /usr/local/tomcat /usr/local/tomcat
COPY ./target/SpringFrameworkInitialize.war /usr/local/tomcat/webapps/ROOT.war
