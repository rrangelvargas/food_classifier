enum DietClassification {
  vegan('Vegan'),
  vegetarian('Vegetarian'),
  neither('Neither'),
  uncertain('Uncertain');

  const DietClassification(this.label);

  final String label;
}
