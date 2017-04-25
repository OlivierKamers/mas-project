import argparse
import re

import networkx as nx
import osmgraph
from networkx.drawing.nx_agraph import write_dot

parser = argparse.ArgumentParser()
parser.add_argument('--input', default='manhattan.osm', help='Path to the input file in .osm format')
parser.add_argument('--output', default='../src/main/resources/maps/manhattan.dot',
                    help='Path to the output file which will be saved in .dot format')
args = parser.parse_args()


def nodes_tag_filter(tags):
  if 'coordinate' not in tags:
    tags.clear()


def ways_tag_filter(tags):
  """
  Filter out roads not accessible to taxis 
  Only keep highway tag (empty tags dict results in way being removed)
  """
  for k in tags.keys():
    if k != 'highway' or tags[k] in ['pedestrian', 'footway', 'cycleway']:
      del tags[k]


def node_coords_to_pos(G):
  coords = nx.get_node_attributes(G, 'coordinate')
  pos = dict(map(lambda (k, v): (k, "{},{}".format(v[0], -v[1])), coords.iteritems()))
  # nx.set_node_attributes(G, 'pos', pos)
  nx.set_node_attributes(G, 'p', pos)
  for node in G.nodes(data=True):
    del node[1]['coordinate']


def main(osmFile, dotFile):
  print "Converting {} to {}.".format(osmFile, dotFile)
  g = osmgraph.parse_file(osmFile, nodes_tag_filter=nodes_tag_filter, ways_tag_filter=ways_tag_filter)
  print 'Number of edges: {}'.format(len(g.edges()))
  print 'Number of nodes: {}'.format(len(g.nodes()))
  node_coords_to_pos(g)
  write_dot(g, dotFile)
  # Remove trailing semicolons from dotFile
  with open(dotFile, "r") as sources:
    lines = sources.readlines()
  with open(dotFile, "w") as sources:
    for line in lines:
      sources.write(re.sub(r';$', '', line))


main(args.input, args.output)
