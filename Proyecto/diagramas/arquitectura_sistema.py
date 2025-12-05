"""
Diagrama de Arquitectura del Sistema DistritoEnRiesgo
Flujo completo desde datos hasta decisión
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.aws.storage import S3
from diagrams.aws.ml import Sagemaker
from diagrams.aws.compute import Lambda, EC2
from diagrams.aws.database import RDS, Dynamodb
from diagrams.aws.network import APIGateway, CloudFront
from diagrams.aws.management import Cloudwatch
from diagrams.aws.integration import SNS, SQS
from diagrams.onprem.client import Users, Client
from diagrams.programming.language import Python
from diagrams.generic.storage import Storage
from diagrams.generic.device import Mobile, Tablet
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

# Diagrama 2: Arquitectura del Sistema Completo
with Diagram("Arquitectura Sistema DistritoEnRiesgo", 
             filename="arquitectura_sistema",
             show=False, 
             direction="LR",
             graph_attr={"fontsize": "18", "bgcolor": "white", "splines": "ortho"}):

    # Usuarios
    with Cluster("USUARIOS"):
        secretaria = Users("Secretaría de\nEducación NY")
        directores = Users("Directores\nde Distrito")
        analistas = Users("Analistas de\nPolíticas")
    
    # Frontend
    with Cluster("FRONTEND"):
        cdn = CloudFront("CDN")
        with Cluster("Dashboard Web"):
            dashboard = Client("React Dashboard")
            mobile = Mobile("App Móvil")
    
    # API Layer
    with Cluster("API LAYER"):
        api = APIGateway("API Gateway")
        with Cluster("Microservicios"):
            auth = Lambda("Auth Service")
            risk = Lambda("Risk Score\nService")
            reports = Lambda("Reports\nService")
            alerts = Lambda("Alerts\nService")
    
    # ML Engine
    with Cluster("ML ENGINE"):
        with Cluster("Modelo Predictivo"):
            endpoint = Sagemaker("SageMaker\nEndpoint")
            model = Storage("RF Model\nv2.1")
        inference = Lambda("Inference\nPipeline")
    
    # Data Layer
    with Cluster("DATA LAYER"):
        with Cluster("Bases de Datos"):
            rds = RDS("PostgreSQL\nDistritos")
            dynamo = Dynamodb("Cache\nPredicciones")
        with Cluster("Data Lake"):
            s3 = S3("S3 Data Lake")
    
    # Eventos y Alertas
    with Cluster("EVENTOS"):
        sns = SNS("Notificaciones")
        sqs = SQS("Cola de\nProcesamiento")
    
    # Monitoreo
    with Cluster("OBSERVABILITY"):
        cw = Cloudwatch("CloudWatch")
    
    # Flujo de usuarios
    secretaria >> cdn
    directores >> cdn
    analistas >> cdn
    
    cdn >> dashboard
    cdn >> mobile
    
    # Flujo API
    dashboard >> api
    mobile >> api
    
    api >> auth
    api >> risk
    api >> reports
    api >> alerts
    
    # Flujo ML
    risk >> inference >> endpoint
    endpoint >> model
    inference >> dynamo
    
    # Flujo datos
    reports >> rds
    rds >> s3
    
    # Flujo alertas
    alerts >> sns
    sns >> Edge(label="email/sms", style="dashed") >> directores
    
    # Monitoreo
    endpoint >> Edge(style="dashed", color="gray") >> cw
    api >> Edge(style="dashed", color="gray") >> cw

print("Diagrama generado: arquitectura_sistema.png")

