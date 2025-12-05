"""
Diagrama MLOps Completo - Sistema DistritoEnRiesgo
Pipeline de Datos Masivos + ML + Aplicación
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.aws.storage import S3
from diagrams.aws.analytics import EMR, Glue, Athena
from diagrams.aws.ml import Sagemaker
from diagrams.aws.compute import Lambda
from diagrams.aws.database import RDS
from diagrams.aws.network import APIGateway
from diagrams.aws.management import Cloudwatch
from diagrams.onprem.client import Users, Client
from diagrams.programming.language import Python
from diagrams.generic.storage import Storage
from diagrams.generic.compute import Rack
from diagrams.generic.device import Mobile
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

# Diagrama 1: Pipeline MLOps Completo
with Diagram("MLOps Pipeline - DistritoEnRiesgo", 
             filename="mlops_distrito_riesgo",
             show=False, 
             direction="TB",
             graph_attr={"fontsize": "20", "bgcolor": "white"}):
    
    # Fuentes de Datos
    with Cluster("1. DATA SOURCES"):
        with Cluster("Datos Educativos"):
            grad_data = Storage("Graduation Rate\n220K registros")
        with Cluster("Datos Census"):
            census_data = Storage("ACS 2017\n3.2K registros")
    
    # Data Lake en S3
    with Cluster("2. DATA LAKE (S3)"):
        raw = S3("Raw Zone\n/data/raw/")
        processed = S3("Processed Zone\n/data/processed/")
        curated = S3("Curated Zone\n/data/curated/")
    
    # Procesamiento Spark
    with Cluster("3. DATA PROCESSING"):
        with Cluster("AWS Glue / SageMaker"):
            spark = Sagemaker("PySpark 3.1\nGlue Sessions")
            
        with Cluster("ETL Pipeline"):
            clean = Python("Limpieza\n• Filtrar District\n• Convertir %\n• Normalizar")
            transform = Python("Transform\n• JOIN datasets\n• Feature eng\n• en_riesgo")
            validate = Python("Validación\n• Schema check\n• Data quality")
    
    # ML Pipeline
    with Cluster("4. ML PIPELINE"):
        with Cluster("Entrenamiento"):
            train = Python("Training\nRandom Forest\nXGBoost")
            tune = Python("Hyperparameter\nTuning")
            evaluate = Python("Evaluación\nF1, AUC, Recall")
        
        with Cluster("Model Registry"):
            registry = Sagemaker("Model Registry\nVersionado")
    
    # Serving
    with Cluster("5. MODEL SERVING"):
        endpoint = Sagemaker("SageMaker\nEndpoint")
        api = APIGateway("API Gateway\n/predict\n/risk-score")
    
    # Aplicación
    with Cluster("6. APP - DistritoEnRiesgo"):
        with Cluster("Dashboard"):
            webapp = Client("Web Dashboard\n• Mapa de riesgo\n• Alertas\n• Reportes")
        with Cluster("Usuarios"):
            admin = Users("Administradores\nEducación NY")
            analysts = Users("Analistas\nPolítica Pública")
    
    # Monitoreo
    with Cluster("7. MONITORING"):
        monitor = Cloudwatch("CloudWatch\nMetrics & Logs")
        alerts = Lambda("Alertas\nSNS")
    
    # Flujo de Datos
    grad_data >> raw
    census_data >> raw
    
    raw >> spark >> clean >> transform >> validate >> processed
    processed >> curated
    
    # Flujo ML
    curated >> train >> tune >> evaluate >> registry
    registry >> endpoint >> api
    
    # Flujo App
    api >> webapp
    webapp >> admin
    webapp >> analysts
    
    # Monitoreo
    endpoint >> Edge(style="dashed", color="gray") >> monitor
    monitor >> Edge(style="dashed", color="red") >> alerts

print("Diagrama generado: mlops_distrito_riesgo.png")

