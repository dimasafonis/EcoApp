#! /bin/python3.9

from tflite_model_maker import image_classifier
from tflite_model_maker.image_classifier import DataLoader

data = DataLoader.from_folder('model/')
train_data, test_data = data.split(0.9)

model = image_classifier.create(train_data)

loss, accuracy = model.evaluate(test_data)

model.export(export_dir='.')
