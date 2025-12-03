"""
Diagrama MLOps - Pipeline de Producción para Predicción de Distritos en Riesgo
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.programming.language import Python
from diagrams.onprem.database import PostgreSQL
from diagrams.onprem.mlops import Mlflow
from diagrams.onprem.container import Docker
from diagrams.onprem.client import Users
from diagrams.onprem.compute import Server
from diagrams.onprem.network import Nginx
from diagrams.generic.storage import Storage
from diagrams.onprem.monitoring import Prometheus, Grafana
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))

with Diagram("MLOps Pipeline - Producción", 
             filename="mlops_pipeline",
             show=False, 
             direction="LR"):
    
    # Ingesta de datos
    with Cluster("1. Data Ingestion"):
        data_source = PostgreSQL("Base de Datos\nEducación NY")
        raw_data = Storage("Raw Data")
    
    # Data Pipeline
    with Cluster("2. Data Pipeline"):
        cleaning = Python("Data Cleaning\n• Filtrar District\n• Convertir tipos\n• Validar schema")
        transform = Python("Transform\n• Feature engineering\n• Normalización")
        validate = Python("Data Validation\n• Check nulls\n• Check ranges")
    
    # Training Pipeline
    with Cluster("3. Training Pipeline"):
        train = Python("Model Training\nRandom Forest")
        optimize = Python("Hyperparameter\nTuning")
        evaluate = Python("Evaluation\nF1 > 0.85")
    
    # Model Registry
    with Cluster("4. Model Registry"):
        mlflow = Mlflow("MLflow")
        model_store = Storage("Model Store\n.pkl versioned")
    
    # Serving
    with Cluster("5. Model Serving"):
        api = Docker("FastAPI\n/predict")
        load_balancer = Nginx("Load Balancer")
        container1 = Docker("Instance 1")
        container2 = Docker("Instance 2")
    
    # Application
    with Cluster("6. Application"):
        app = Server("App STEM\nCareers")
        users = Users("Students")
    
    # Monitoring
    with Cluster("7. Monitoring & Retraining"):
        prometheus = Prometheus("Metrics")
        grafana = Grafana("Dashboard")
        retrain = Python("Retrain\nTrigger")
    
    # Data Flow
    data_source >> raw_data >> cleaning >> transform >> validate
    
    # Training Flow
    validate >> Edge(label="features") >> train >> optimize >> evaluate
    evaluate >> Edge(label="log") >> mlflow
    evaluate >> Edge(label="save") >> model_store
    
    # Serving Flow
    model_store >> Edge(label="load") >> api
    load_balancer >> container1
    load_balancer >> container2
    api >> load_balancer
    
    # Application Flow
    container1 >> Edge(label="predictions") >> app
    container2 >> app
    app >> users
    
    # Monitoring Flow
    api >> Edge(style="dashed") >> prometheus >> grafana
    grafana >> Edge(label="alert", style="dashed", color="red") >> retrain
    retrain >> Edge(style="dashed", color="red") >> train

print("✅ Diagrama generado: mlops_pipeline.png")
