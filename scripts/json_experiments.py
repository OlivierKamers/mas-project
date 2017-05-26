import fnmatch
import json
import os

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


def make_double_plot(timestamp, j, title, k1, k2, xlabel='Tick'):
  fig, ax1 = plt.subplots()

  ax1.plot(j[k1], 'r-')
  ax1.set_xlabel(xlabel)
  ax1.set_ylabel(k1, color='r')
  ax1.tick_params('y', colors='r')

  ax2 = ax1.twinx()
  ax2.plot(j[k2], 'b--')
  ax2.set_ylabel(k2, color='b')
  ax2.tick_params('y', colors='b')

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
