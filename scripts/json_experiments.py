import fnmatch
import json
import os
from math import ceil

import numpy as np
from matplotlib import pyplot as plt

path = os.path.join('..', 'stats')


def analyze_experiment(filename):
  with open(os.path.join(path, filename), 'r') as f:
    j = json.load(f)
    timestamp = filename.split('_')[1].split('.')[0]
    print timestamp
    print j
    print "\n".join(j.keys())
    make_plots(timestamp, j)


def make_plots(timestamp, j):
  make_double_plot(timestamp, j, 'Amount of idle taxis and waiting customers', 'amountOfIdleTaxis', 'amountOfWaitingCustomers')


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
  ax1.tick_params('y', colors='r')

  ax2 = ax1.twinx()
  y2 = average(j[k2], sample)
  x2 = np.linspace(0, x_max, num=len(y2))
  ax2.plot(x2, y2, 'b-')
  ax2.set_ylabel(k2, color='b')
  ax2.tick_params('y', colors='b')

  plt.ylim(ymin=0)

  plt.title(title)
  fig.tight_layout()
  # plt.show()
  plt.savefig(os.path.join('..', 'figures', '{}_{}_{}.png'.format(timestamp, k1, k2)), dpi=300)
  plt.close('all')


def main():
  for f in os.listdir(path):
    if fnmatch.fnmatch(f, 'stats_*.json'):
      analyze_experiment(f)


if __name__ == "__main__":
  main()
