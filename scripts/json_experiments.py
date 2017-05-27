import fnmatch
import json
import os
from math import ceil

import numpy as np
from matplotlib import pyplot as plt

stats_path = os.path.join('..', 'stats')
figs_path = os.path.join('..', 'figures')


def cleanup(j):
  j['pickupWaitingTimes'] = map(lambda x: 1.0 * x / 60000, j['pickupWaitingTimes'])


def analyze_experiment(filename):
  with open(os.path.join(stats_path, filename), 'r') as f:
    j = json.load(f)
    timestamp = filename.split('_')[1].split('.')[0]
    print timestamp
    print j
    print "\n".join(j.keys())
    cleanup(j)
    make_plots(timestamp, j)


def make_histogram(timestamp, j, title, k, xlabel):
  if len(j[k]) == 0: return
  plt.hist(j[k], 50, facecolor='green', alpha=0.75)
  plt.xlabel(xlabel)
  plt.ylabel('Count')
  plt.title(title)
  plt.grid(True)

  # plt.show()
  plt.savefig(os.path.join(figs_path, '{}_{}_{}.png'.format(timestamp, k, 'hist')), dpi=300)
  plt.close('all')


def make_plots(timestamp, j):
  make_double_plot(timestamp, j, 'Amount of idle taxis and waiting customers', 'amountOfIdleTaxis', 'amountOfWaitingCustomers')
  make_histogram(timestamp, j, 'Customer waiting time before pickup', 'pickupWaitingTimes', 'Waiting time (min)')
  make_histogram(timestamp, j, 'Route length reduction by trading', 'tradeProfits', 'Trade profit (km)')
  make_histogram(timestamp, j, 'Number of requests before pickup', 'numberOfRequests', 'Count')
  make_histogram(timestamp, j, 'Ratio of actual distance to shortest distance', 'travelTimeOverhead', 'Ratio')


def average(arr, n):
  end = n * int(len(arr) / n)
  return np.mean(np.asarray(arr)[:end].reshape(-1, n), 1)


def make_double_plot(timestamp, j, title, k1, k2, xlabel='Tick'):
  fig, ax1 = plt.subplots()

  points = 500
  x_max = max(len(j[k1]), len(j[k2]))
  sample = int(ceil(1.0 * x_max / points))

  y1 = average(j[k1], sample)
  x1 = np.linspace(0, x_max, num=len(y1))
  ax1.plot(x1, y1, 'r-')
  ax1.set_xlabel(xlabel)
  ax1.set_ylabel(k1, color='r')
  ax1.margins(0, 0.1)
  ax1.tick_params('y', colors='r')

  ax2 = ax1.twinx()
  y2 = average(j[k2], sample)
  x2 = np.linspace(0, x_max, num=len(y2))
  ax2.plot(x2, y2, 'b-')
  ax2.set_ylabel(k2, color='b')
  ax2.margins(0, 0.1)
  ax2.tick_params('y', colors='b')

  plt.title(title)
  # fig.tight_layout()
  # plt.show()
  plt.savefig(os.path.join(figs_path, '{}_{}_{}.png'.format(timestamp, k1, k2)), dpi=300)
  plt.close('all')


def main():
  for f in os.listdir(stats_path):
    if fnmatch.fnmatch(f, 'stats_*.json'):
      analyze_experiment(f)


if __name__ == "__main__":
  main()
